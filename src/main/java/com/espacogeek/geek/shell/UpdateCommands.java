package com.espacogeek.geek.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.espacogeek.geek.data.impl.MovieControllerImpl;
import com.espacogeek.geek.data.impl.SerieControllerImpl;

@ShellComponent
public class UpdateCommands {
    private final MovieControllerImpl movieController;
    private final SerieControllerImpl serieController;

    public UpdateCommands(MovieControllerImpl movieController, SerieControllerImpl serieController) {
        this.movieController = movieController;
        this.serieController = serieController;
    }

    @ShellMethod(key = "update-movies", value = "Run movie update now")
    public String updateMoviesNow() {
        try {
            movieController.updateMoviesNow();
            return "Movie update triggered";
        } catch (Exception e) {
            return "Movie update failed: " + e.getMessage();
        }
    }

    @ShellMethod(key = "update-series", value = "Run series update now")
    public String updateSeriesNow() {
        try {
            serieController.updateTvSeriesNow();
            return "Series update triggered";
        } catch (Exception e) {
            return "Series update failed: " + e.getMessage();
        }
    }

    @ShellMethod(key = "update-all", value = "Run all updates now")
    public String updateAllNow() {
        try {
            movieController.updateMoviesNow();
            serieController.updateTvSeriesNow();
            return "All updates triggered";
        } catch (Exception e) {
            return "All updates failed: " + e.getMessage();
        }
    }
}
