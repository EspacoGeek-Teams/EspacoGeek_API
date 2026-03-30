package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.models.MediaStatusModel;
import com.espacogeek.geek.services.MediaStatusService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MediaStatusController {

    private final MediaStatusService mediaStatusService;

    /**
     * Retrieves all available media statuses.
     *
     * @return A list of all MediaStatusModel objects.
     */
    @QueryMapping(name = "findAllMediaStatuses")
    public List<MediaStatusModel> findAllMediaStatuses() {
        return mediaStatusService.findAll();
    }
}
