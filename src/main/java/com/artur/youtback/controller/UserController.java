package com.artur.youtback.controller;

import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.User;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.utils.Cookies;
import com.artur.youtback.utils.Path;
import com.artur.youtback.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Arrays.stream;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    TokenService tokenService;



    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long id){
        try{
            if(id != null){
                User user = userService.findById(id);
                //System.out.println( user.getPassword() + " MATCHES ? " + passwordEncoder.matches("11111asxdasxd1s11", user.getPassword()));
                return ResponseEntity.ok(user);
            }
            else {
                return ResponseEntity.ok(userService.findAll());
            }
        }catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/videos")
    public ResponseEntity<?> getUserVideos(@RequestParam(name = "userId") Long userId, @RequestParam(required = false, name = "sortOption") Integer sortOption){
        try {
            return ResponseEntity.ok(userService.getAllUserVideos(userId,sortOption != null ?  Utils.processSortOptions(sortOption) : null));
        } catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/liked")
    public ResponseEntity<?> hasUserLikedVideo(@RequestParam(name = "userId") Long userId, @RequestParam(name = "videoId")Long videoId){
        try {
            return ResponseEntity.ok(userService.hasUserLikedVideo(userId,videoId));
        } catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> validateUser(HttpServletRequest request){
        Optional<Cookie> optionalCookie = Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(Cookies.REFRESH_TOKEN)).findAny();
        if(optionalCookie.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Cookie refreshCookie = optionalCookie.get();
        if(!tokenService.isRefreshTokenValid(refreshCookie.getValue())){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        User user;
        try{
            user = userService.findById(User.deserialize(tokenService.decode(refreshCookie.getValue()).getSubject()).getId());
        }catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        String accessToken = tokenService.generateAccessToken(user);
        return ResponseEntity.ok(accessToken);
    }

    @GetMapping("/subscribed")
    public ResponseEntity<?> hasUserSubscribedChannel(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId")Long channelId){
        try {
            return ResponseEntity.ok(userService.hasUserSubscribedChannel(userId,channelId));
        } catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?>  loginByEmailAndPassword(Authentication authentication,@Autowired HttpServletRequest request,@Autowired HttpServletResponse response){
        if(authentication == null){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
        }
        String token;
        User user = (User)authentication.getPrincipal();
        response.addCookie(Cookies.refreshCookie(tokenService.generateRefreshToken(user)));
        token = tokenService.generateAccessToken(user);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(@RequestParam("email") String email, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("imageFile") MultipartFile profileImage, @Autowired BCryptPasswordEncoder passwordEncoder, @Autowired HttpServletResponse response){
        try {
            String profilePicturePath = userService.saveImage(profileImage);
            User user = User.create(email, username, passwordEncoder.encode(password), profilePicturePath);
            User registeredUser = userService.registerUser(user);

            response.addCookie(Cookies.refreshCookie(tokenService.generateRefreshToken(registeredUser)));
            response.addHeader("accessToken", tokenService.generateAccessToken(registeredUser));
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        }catch (ExistedUserException | UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }

    @PostMapping("/search-history")
    public ResponseEntity<String> addSearchOptionToUserById(@RequestParam(name = "userId") Long userId, @RequestBody SearchHistory searchOption){
        try{
            userService.addSearchOption(userId, searchOption.getSearchOption());
            return ResponseEntity.status(HttpStatus.OK).body("Search option added");
        }catch (UserNotFoundException  e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ExistingSearchOptionException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("imageFile") MultipartFile file){
        try{
            userService.saveImage(file);
            return ResponseEntity.ok(null);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("")
    public ResponseEntity<String> update(@RequestBody User user, @RequestParam(name = "userId") Long userId){
        try{
            userService.update(user, userId);
            return ResponseEntity.ok(null);
        }catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/like")
    public ResponseEntity<?> likeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId", required = false) Long userId){
        try{
            userService.likeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (VideoNotFoundException | UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/dislike")
    public ResponseEntity<?> dislikeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId", required = false) Long userId){
        try{
            userService.dislikeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/subscribe")
    public ResponseEntity<?> subscribeById(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId") Long subscribedChannel){
        try {
            userService.subscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeById(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId") Long subscribedChannel){
        try {
            userService.unsubscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "userId") Long id){
        try{
            userService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("search-history")
    public ResponseEntity<String> deleteSearchOption(@RequestParam(name = "userId") Long id, @RequestBody SearchHistory searchHistory){
        try{
            userService.deleteSearchOption(id, searchHistory.getSearchOption());
            return ResponseEntity.ok("Deleted");
        }catch (SearchOptionNotFoundException | UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
