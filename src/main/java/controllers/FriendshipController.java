package controllers;


import DTO.ApiResponse;
import DTO.ApiResponseWrapper;
import DTO.FriendshipDTO;
import model.Friendship;
import model.FriendshipStatus;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import repos.FriendshipRepo;
import repos.UserRepo;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controller responsible for friendship management.
 *
 * <p>This controller provides endpoints for sending friendship requests,
 * viewing incoming and outgoing pending requests, accepting or rejecting
 * requests, and retrieving the list of accepted friends of the authenticated user.</p>
 */
@RestController
@RequestMapping("/user")
public class FriendshipController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    FriendshipRepo friendshipRepo;



    /*
    @GetMapping("/friends")
    public ApiResponse getAllFriends(@RequestBody String username){

    }
    */

    /**
     * Creates a new friendship request from the authenticated user to another user.
     *
     * <p>The authenticated user is obtained from the {@link Principal} object.
     * The username of the target user is read from the request body under
     * {@code addressee_username}. The request is rejected if one of the users
     * does not exist, if the user tries to send a request to themselves,
     * or if a friendship request already exists between the two users.</p>
     *
     * @param principal authenticated user
     * @param request map containing the target username under {@code addressee_username}
     * @return an {@link ApiResponse} containing the result of the operation
     */
    @PostMapping("/make_request")
    public ApiResponse makeRequest(Principal principal, @RequestBody Map<String, String> request) {
        String requesterUsername = principal.getName();
        String addresseeUsername = request.get("addressee_username").trim();

        System.out.println(requesterUsername + "   requester       " + addresseeUsername);

        User requester = userRepo.findByUsername(requesterUsername);
        User addressee = userRepo.findByUsername(addresseeUsername);

        if (requester == null || addressee == null) {
            return new ApiResponse("user not found", null);
        }

        if (requester.getId().equals(addressee.getId())) {
            return new ApiResponse("cannot add yourself", null);
        }

        boolean exists = friendshipRepo.existsByRequester_IdAndAddressee_Id(requester.getId(), addressee.getId())
                || friendshipRepo.existsByRequester_IdAndAddressee_Id(addressee.getId(), requester.getId());

        if (exists) {
            return new ApiResponse("request already exists", null);
        }

        friendshipRepo.save(new Friendship(requester, addressee));
        return new ApiResponse("request successfully created", null);
    }


    /**
     * Returns all incoming pending friendship requests for the authenticated user.
     *
     * <p>This endpoint finds all friendship requests where the authenticated user
     * is the addressee and the request status is {@code PENDING}. The result
     * is converted to a list of {@link FriendshipDTO} objects.</p>
     *
     * @param principal authenticated user
     * @return an {@link ApiResponseWrapper} containing the list of incoming friendship requests
     */

    @GetMapping("/friend-requests/incoming")
    public ApiResponseWrapper<List<FriendshipDTO>> incoming(Principal principal) {
        User me = userRepo.findByUsername(principal.getName());

        if (me==null) {
            return ApiResponseWrapper.error("null user");
        }


        List<Friendship> requests = friendshipRepo.findByAddressee_IdAndStatus(me.getId(), FriendshipStatus.PENDING);


        List<FriendshipDTO> dto = requests.stream()
                .map(fr -> new FriendshipDTO(
                        fr.getId(),
                        fr.getRequester().getUsername(),
                        fr.getStatus().toString(),
                        fr.getFriendshipSentAt()
                ))
                .toList();

        return ApiResponseWrapper.ok(dto);
    }
    /**
     * Returns all outgoing pending friendship requests created by the authenticated user.
     *
     * <p>This endpoint finds all friendship requests where the authenticated user
     * is the requester and the request status is {@code PENDING}. The result
     * is converted to a list of {@link FriendshipDTO} objects.</p>
     *
     * @param principal authenticated user
     * @return an {@link ApiResponseWrapper} containing the list of outgoing friendship requests
     */
    @GetMapping("/friend-requests/outgoing")
    public ApiResponseWrapper<List<FriendshipDTO>> outgoing(Principal principal){
        User me = userRepo.findByUsername(principal.getName());

        if (me == null) {
            return ApiResponseWrapper.error("null user");
        }
        List <Friendship> requests = friendshipRepo.findByRequester_IdAndStatus(me.getId(), FriendshipStatus.PENDING);
        List<FriendshipDTO> dto = requests.stream()
                .map(fr -> new FriendshipDTO(
                        fr.getId(),
                        fr.getAddressee().getUsername(),
                        fr.getStatus().toString(),
                        fr.getFriendshipSentAt()
                )).toList();

        return ApiResponseWrapper.ok(dto);

    }
    /**
     * Accepts an incoming friendship request for the authenticated user.
     *
     * <p>The requester username is read from the request body under
     * {@code requester_username}. If the corresponding friendship request exists,
     * its status is changed to {@code ACCEPTED}.</p>
     *
     * @param principal authenticated user
     * @param api map containing the requester username under {@code requester_username}
     * @return an {@link ApiResponse} containing the result of the operation
     */
    @PutMapping("/friend-requests/accept")
    public ApiResponse acceptFriendship(Principal principal, @RequestBody Map<String, String> api){


        Friendship friendship =  getFriendship(principal, api);

        if (friendship== null) {
            return new ApiResponse("this friedship never existed",null);
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepo.save(friendship);
        return new ApiResponse("friendship accepted", null);
    }

    /**
     * Rejects an incoming friendship request for the authenticated user.
     *
     * <p>The requester username is read from the request body under
     * {@code requester_username}. If the corresponding friendship request exists,
     * its status is changed to {@code REJECTED}.</p>
     *
     * @param principal authenticated user
     * @param api map containing the requester username under {@code requester_username}
     * @return an {@link ApiResponse} containing the result of the operation
     */

    @PutMapping("/friend-requests/reject")
    public ApiResponse rejectFriendship(Principal principal, @RequestBody Map<String,String> api){

        Friendship friendship = getFriendship(principal, api);
        if (friendship == null) {
            return new ApiResponse("this friendship neber existed", null);
        }
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepo.save(friendship);
        return new ApiResponse("friendship rejected", null);
    }
    /**
     * Returns all accepted friendships of the authenticated user.
     *
     * <p>This endpoint finds all friendship relationships with status
     * {@code ACCEPTED} where the authenticated user is either the requester
     * or the addressee. The result is converted to a list of
     * {@link FriendshipDTO} objects containing the username of the other user.</p>
     *
     * @param principal authenticated user
     * @return an {@link ApiResponseWrapper} containing the list of accepted friendships
     */
    @GetMapping("/friends")
    public ApiResponseWrapper<List<FriendshipDTO>> friendship(Principal principal){
        System.out.println("\nfriends\n");
        String username = principal.getName();

        if (username == null) {
            return ApiResponseWrapper.error("empty username");
        }


        User user = userRepo.findByUsername(username);
        List<Friendship> friendships =
                friendshipRepo.findByStatusAndRequester_IdOrStatusAndAddressee_Id(
                        FriendshipStatus.ACCEPTED, user.getId(),
                        FriendshipStatus.ACCEPTED, user.getId()
                );

        List<FriendshipDTO> dto = friendships.stream()
                .map(fr -> {
                    String friendUsername =
                            fr.getRequester().getId().equals(user.getId())
                                    ? fr.getAddressee().getUsername()
                                    : fr.getRequester().getUsername();

                    return new FriendshipDTO(
                            fr.getId(),
                            friendUsername,
                            fr.getStatus().toString(),
                            fr.getFriendshipSentAt()
                    );
                })
                .toList();

        return ApiResponseWrapper.ok(dto);

    }
    /**
     * Removes an accepted friendship between the authenticated user and another user.
     *
     * <p>The authenticated user is obtained from the {@link Principal} object,
     * while the username of the friend to remove is provided in the request body
     * under {@code friend_username}. The method searches for an accepted friendship
     * in both directions and deletes it if found.</p>
     *
     * @param principal authenticated user
     * @param api map containing the friend's username under {@code friend_username}
     * @return an {@link ApiResponseWrapper} containing the result of the operation
     */
    @DeleteMapping("/friends/remove")
    public ApiResponseWrapper<String> removeFriend(Principal principal, @RequestBody Map<String, String> api) {
        String myUsername = principal.getName();
        String friendUsername = api.get("friend_username");

        if (friendUsername == null || friendUsername.trim().isEmpty()) {
            return ApiResponseWrapper.error("friend username is required");
        }

        User me = userRepo.findByUsername(myUsername);
        User friend = userRepo.findByUsername(friendUsername.trim());

        if (me == null || friend == null) {
            return ApiResponseWrapper.error("user not found");
        }

        Friendship friendship =
                friendshipRepo.findByRequester_IdAndAddressee_IdOrRequester_IdAndAddressee_Id(
                        me.getId(), friend.getId(),
                        friend.getId(), me.getId()
                );

        if (friendship == null) {
            return ApiResponseWrapper.error("friendship not found");
        }

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            return ApiResponseWrapper.error("users are not friends");
        }

        friendshipRepo.delete(friendship);

        return ApiResponseWrapper.ok("friend removed successfully");
    }
    /**
     * Finds a friendship request between the provided requester and the authenticated user.
     *
     * <p>This helper method is used when accepting or rejecting friendship requests.
     * The requester username is read from the request body, while the addressee
     * username is obtained from the authenticated user.</p>
     *
     * @param principal authenticated user
     * @param api map containing the requester username under {@code requester_username}
     * @return the matching {@link Friendship} if it exists; otherwise {@code null}
     */
    private Friendship getFriendship(Principal principal,Map<String,String> api){
        String requesterUsername = api.get("requester_username");
        String addresseeUsername = principal.getName();

        if (requesterUsername == null || addresseeUsername == null) return null;


        User requester = userRepo.findByUsername(requesterUsername);
        User addressee = userRepo.findByUsername(addresseeUsername);

        if (requester == null || addressee == null) return null;

        Friendship friendship =  friendshipRepo.findByRequester_IdAndAddressee_Id(requester.getId(), addressee.getId());
        return friendship;
    }




}
