package com.artur.youtback.controller;


import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.User;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.FindOptions;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.Utils;
import com.healthmarketscience.jackcess.util.OleBlob;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.hibernate.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.service.annotation.PutExchange;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class VideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    JwtDecoder jwtDecoder;


    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long videoId, @RequestParam(required = false, name = "sortOption") Integer sortOption, @RequestParam(value = "option", required = false) String option, @RequestParam(value = "value", required = false)String value, HttpServletRequest request, Authentication authentication) {
        if(videoId != null){
            try {
                return ResponseEntity.ok(videoService.findById(videoId));
            } catch(VideoNotFoundException exception){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else{
            try{
                String languages = request.getHeader("User-Languages");
                if(languages.isEmpty()) throw new IllegalArgumentException("Here should find by country");
                String subject = null;
                if(authentication != null){
                    JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
                    subject = jwt.getToken().getSubject();
                }
                return ResponseEntity.ok(videoService.recommendations(subject != null ? Long.parseLong(subject) : null, languages.split(",")));
            } catch (IllegalArgumentException e){
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(e.getMessage());
            }
//            return findAll(sortOption != null ? Utils.processSortOptions(sortOption) : null);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(){
        videoService.testMethod();
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
        System.out.println("m3u8 request");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=index.m3u8");
        try{
            FileSystemResource resource = new FileSystemResource(videoService.m3u8Index(videoId));
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping(value = "/{filename}", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<?> ts(@PathVariable String filename){
        System.out.println("GETTING TS " + filename);
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
                e.printStackTrace();
            }
            Video video = videoService.watchById(videoId, jwt == null ? null : jwt.getSubject());
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }catch ( VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("")
    public ResponseEntity<String> create(@RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("thumbnail")MultipartFile thumbnail, @RequestParam("video")MultipartFile video, HttpServletRequest request){
        try {
            String accessToken = request.getHeader("accessToken");
            if(accessToken == null || !tokenService.isTokenValid(accessToken)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            long userId = Long.parseLong(tokenService.decode(accessToken).getSubject());
            videoService.create(title, description, thumbnail, video, userId);
            return ResponseEntity.status(HttpStatus.OK).body("Created");
        }catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e){
           e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }

    @PutMapping("")
    public ResponseEntity<String> update(@RequestParam(value = "videoId")Long videoId, @RequestParam(name = "duration", required = false)String duration, @RequestParam(name = "title", required = false)String title, @RequestParam(name = "description", required = false) String description, @RequestParam(name = "thumbnail", required = false)MultipartFile thumbnail){
        try{
            videoService.update(videoId, title, description, duration, thumbnail);
            return ResponseEntity.ok(null);
        }catch(VideoNotFoundException | IOException e){
            return ResponseEntity.badRequest().body(null);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



}


