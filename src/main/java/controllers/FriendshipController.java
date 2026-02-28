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
    public ApiResponse makeRequest(@RequestBody Map<String, String> request){
        String requesterUsername = request.get("requester_username");
        String addresseeUsername = request.get("addressee_username");

        model.User requester = userRepo.findByUsername(requesterUsername);
        model.User addressee = userRepo.findByUsername(addresseeUsername);


        if (friendshipRepo.existsByRequester_IdAndAddressee_Id(requester.getId(),addressee.getId())) {
            return new ApiResponse("the request already exist or declined",null);
        }

        friendshipRepo.save(new Friendship(requester,addressee));
        return new ApiResponse("request succesfully created",null);
    }

    @GetMapping("/friend-requests/incoming")
    public ApiResponseWrapper<List<FriendshipDTO>> incoming(@RequestParam String username) {
        User me = userRepo.findByUsername(username);

        if (me==null) {
            return ApiResponseWrapper.error("null user");
        }


        List<Friendship> requests = friendshipRepo.findByAddressee_IdAndStatus(me.getId(), FriendshipStatus.PENDING);
        // або твій findRequestsToMe(me.getId())


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

    @GetMapping("friend-requests/outgoing")
    public ApiResponseWrapper<List<FriendshipDTO>> outgoing(@RequestParam String username){
        User me = userRepo.findByUsername(username);

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


}
