package com.artur.youtback.controller;


import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.FindOptions;
import com.artur.youtback.utils.SortOption;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    public ResponseEntity<?> find(@RequestParam(required = false) Long videoId, @RequestParam(required = false, name = "sortOption") Integer sortOption, @RequestParam(value = "option", required = false) String option, @RequestParam(value = "value", required = false)String value,@RequestParam(required = false) Set<Long> excludes, HttpServletRequest request, Authentication authentication) {
        if(videoId != null){
            try {
                return ResponseEntity.ok(videoService.findById(videoId));
            } catch(VideoNotFoundException exception){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else{
            try{
                long start = System.currentTimeMillis();
                String languages = request.getHeader("User-Languages");
                if(languages.isEmpty()) throw new IllegalArgumentException("Here should find by country");
                String subject = null;
                if(authentication != null){
                    JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
                    subject = jwt.getToken().getSubject();
                }
                Collection<?> videos = videoService.recommendations(subject != null ? Long.parseLong(subject) : null,excludes, languages.split(","));
                logger.trace("Recommendations done in " + ((float) (System.currentTimeMillis() - start) / 1000) + "s");
                return ResponseEntity.ok(videos);
            } catch (IllegalArgumentException e){
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(e.getMessage());
            }
//            return findAll(sortOption != null ? Utils.processSortOptions(sortOption) : null);
        }
    }

    @GetMapping("/test")
    @RolesAllowed("ADMIN")
    public ResponseEntity<?> test(){
        logger.trace("TEST METHOD CALLED");
        //videoService.testMethod();
        return ResponseEntity.ok(null);
    }

    private ResponseEntity<List<Video>> findAll(SortOption sortOption){
        try{
            return ResponseEntity.ok(videoService.findAll(sortOption));
        }catch(VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> findByOption(@RequestParam(value = "option") String option, @RequestParam(value = "value", required = false)String value){
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
            return ResponseEntity.ok(videoService.findByOption(FindOptions.VideoOptions.BY_TITLE.name(), searchQuery));
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/download", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<?> downloadVideo(@RequestParam("videoId") Long videoId){
        //            InputStreamResource inputStreamResource = new InputStreamResource(videoService.getVideoStreamById(videoId));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=index.m3u8");
        try{
            FileSystemResource resource = new FileSystemResource(videoService.m3u8Index(videoId));
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch(Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping(value = "/{filename}", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<?> ts(@PathVariable String filename){
        return ResponseEntity.ok(new FileSystemResource(videoService.ts(filename)));
    }

    @GetMapping("/watch")
    public ResponseEntity<Video> watchVideoById(@RequestParam(name = "videoId") Long videoId, HttpServletRequest request){
        try{
            String token = request.getHeader("AccessToken");
            Jwt jwt = null;
            try{
                jwt = jwtDecoder.decode(token.substring(token.indexOf(" ")));
            }catch (JwtException e){
                logger.error(e.getMessage());
            }
            Video video = videoService.watchById(videoId, jwt == null ? null : jwt.getSubject());
            return ResponseEntity.ok(video);
        }catch ( VideoNotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("")
    public ResponseEntity<String> create(@ModelAttribute VideoCreateRequest video, HttpServletRequest request){
        try {
            String accessToken = request.getHeader("accessToken");
            if(accessToken == null || !tokenService.isTokenValid(accessToken)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            long userId = Long.parseLong(tokenService.decode(accessToken).getSubject());
            videoService.create(video, userId);
            return ResponseEntity.ok("Created");
        }catch(UserNotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addVideos(@RequestParam("a") Integer amount){
        try {
            return ResponseEntity.ok(videoService.addVideos(amount));
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("")
    public ResponseEntity<?> update(@ModelAttribute VideoUpdateRequest updateRequest){
        try{
            videoService.update(updateRequest);
            return ResponseEntity.ok(null);
        }catch(VideoNotFoundException  e){
            return ResponseEntity.notFound().build();
        }catch (IOException | InterruptedException e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "videoId") Long id){
        try{
            videoService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IOException e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



}


