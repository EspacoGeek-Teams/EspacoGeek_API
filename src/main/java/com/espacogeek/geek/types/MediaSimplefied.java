package com.espacogeek.geek.types;

import java.util.ArrayList;
import java.util.List;

import com.espacogeek.geek.models.MediaModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaSimplefied {
    private Integer id;
    private String name;
    private String cover;

    public static MediaSimplefied fromMediaModel(MediaModel mediaModel) {
        MediaSimplefied mediaSimplefied = new MediaSimplefied();
        mediaSimplefied.setId(mediaModel.getId());
        mediaSimplefied.setName(mediaModel.getName());
        mediaSimplefied.setCover(mediaModel.getCover());
        return mediaSimplefied;
    }

    public static List<MediaSimplefied> fromMediaModelList(List<MediaModel> mediaModels) {
        List<MediaSimplefied> mediaSimplefieds = new ArrayList<>();
        for (MediaModel mediaModel : mediaModels) {
            mediaSimplefieds.add(fromMediaModel(mediaModel));
        }
        return mediaSimplefieds;
    }
}
