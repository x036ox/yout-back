package com.artur.youtback.controller;


import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.FindOptions;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    JwtDecoder jwtDecoder;

    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long videoId,
                                  @RequestParam(required = false, name = "sortOption") Integer sortOption,
                                  @RequestParam(required = false, defaultValue = "0") Integer page,
                                  @RequestParam(required = false) Integer size,
                                  HttpServletRequest request,
                                  Authentication authentication) {
        if(videoId != null){
            try {
                return ResponseEntity.ok(videoService.findById(videoId));
            } catch(NotFoundException exception){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else{
            try{
                long start = System.currentTimeMillis();
                String languages = request.getHeader("User-Languages");
                if(languages.isEmpty()) throw new IllegalArgumentException("User languages should not be empty");
                String subject = null;
                if(authentication != null){
                    JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
                    subject = jwt.getToken().getSubject();
                }
                List<Video> videos = videoService.recommendations(
                        subject != null ? Long.parseLong(subject) : null,
                        page,
                        languages.split(","),
                        size == null ? AppConstants.MAX_VIDEOS_PER_REQUEST : size,
                        Utils.processSortOptions(sortOption)
                        );
                logger.trace("Recommendations done in " + ((float) (System.currentTimeMillis() - start) / 1000) + "s");
                return ResponseEntity.ok(videos);
            } catch (IllegalArgumentException e){
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(e.getMessage());
            }
        }
    }

//    @GetMapping("/test")
//    @RolesAllowed("ADMIN")
//    public ResponseEntity<?> test(){
//        logger.trace("TEST METHOD CALLED");
//        videoService.testMethod();
//        return ResponseEntity.ok(null);
//    }

    private ResponseEntity<List<Video>> findAll(SortOption sortOption){
        try{
            return ResponseEntity.ok(videoService.findAll(sortOption));
        }catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> findByOption(
            @RequestParam List<String> option,
            @RequestParam List<String> value
    ){
        try{
            return ResponseEntity.ok(videoService.findByOption(option, value));
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(@RequestParam(value = "search_query") String searchQuery){
        try{
            return ResponseEntity.ok(videoService.findByOption(
                    Collections.singletonList(FindOptions.VideoOptions.BY_TITLE.name()),
                    Collections.singletonList(searchQuery))
            );
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/{id}/index.m3u8", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> m3u8Index(@PathVariable Long id){
        HttpHeaders headers = new HttpHeaders();
        try{
            headers.set("Content-Type", "application/vnd.apple.mpegurl");
            return new ResponseEntity<>(new InputStreamResource(videoService.m3u8Index(id)), headers, HttpStatus.OK);
        } catch(NotFoundException e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping(value = "/{id}/{ts}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> ts(@PathVariable Long id, @PathVariable String ts){
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.set("Content-Type", "application/vnd.apple.mpegurl");
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new InputStreamResource(videoService.ts(id, ts)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/watch")
    public ResponseEntity<Video> watchVideoById(@RequestParam(name = "videoId") Long videoId, HttpServletRequest request, JwtAuthenticationToken jwtAuthenticationToken){
        try{
            Jwt jwt = null;
            if(jwtAuthenticationToken != null){
                jwt =  jwtAuthenticationToken.getToken();
            }
            Video video = videoService.watchById(videoId, jwt == null ? null : Long.parseLong(jwt.getSubject()));
            return ResponseEntity.ok(video);
        }catch ( NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("")
    public ResponseEntity<?> create(@ModelAttribute VideoCreateRequest video, HttpServletRequest request, Authentication auth){
        try {
            if(!(auth instanceof JwtAuthenticationToken)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            long userId =  Long.parseLong(((JwtAuthenticationToken) auth).getToken().getSubject());
            videoService.create(video, userId);
            return ResponseEntity.ok(null);
        }catch(NotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addVideos(@RequestParam("a") Integer amount){
        try {
            return ResponseEntity.ok(videoService.addVideos(amount));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("")
    public ResponseEntity<?> update(@ModelAttribute VideoUpdateRequest updateRequest){
        try{
            videoService.update(updateRequest);
            return ResponseEntity.ok(null);
        }catch(NotFoundException  e){
            return ResponseEntity.notFound().build();
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "videoId") Long id){
        try{
            videoService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}


