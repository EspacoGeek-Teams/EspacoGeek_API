package com.espacogeek.geek.types;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaPage {
    private List<MediaSimplefied> content;
    private int totalPages;
    private long totalElements;
    private int number;
    private int size;
}
