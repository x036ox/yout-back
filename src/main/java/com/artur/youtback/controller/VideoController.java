package com.artur.youtback.controller;


import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.Utils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.PutExchange;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@CrossOrigin
public class VideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserService userService;


    private ResponseEntity<List<Video>> findAll(SortOption sortOption){

        try{
            return ResponseEntity.ok(videoService.findAll(sortOption));
        }catch(VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long videoId, @RequestParam(required = false, name = "sortOption") Integer sortOption) {
        if(videoId != null){
            try {
                return ResponseEntity.ok(videoService.findById(videoId));
            } catch(VideoNotFoundException exception){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        }
        else{
            return findAll(sortOption != null ? Utils.processSortOptions(sortOption) : null);
        }


    }

    @PostMapping("")
    public ResponseEntity<String> create(@Valid @RequestBody Video video, @RequestParam Long userId){
        try {
            videoService.create(video, userId);
            return ResponseEntity.status(HttpStatus.OK).body("Created");
        }catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PutMapping("")
    public ResponseEntity<String> update(@RequestBody Video video, @RequestParam(name = "videoId") Long videoId){
        try{
            videoService.update(video, videoId);
            return ResponseEntity.ok(null);
        }catch(VideoNotFoundException e){
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
        }
    }

    @GetMapping("/watch")
    public ResponseEntity<Video> watchVideoById(@RequestParam(name = "videoId") Long videoId){
        try{
            Video video = videoService.watchById(videoId);
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }catch ( VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }



}


