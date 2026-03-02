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

    @GetMapping("/friends")
    public ApiResponseWrapper<List<FriendshipDTO>> friendship(Principal principal){
        String username = principal.getName();
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
