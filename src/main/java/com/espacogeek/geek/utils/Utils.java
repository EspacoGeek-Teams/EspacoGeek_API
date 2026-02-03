package com.espacogeek.geek.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;

public abstract class Utils {

    /**
     * Extracts the user ID from the given authentication object. The user ID is
     * assumed
     * to be stored in an authority with the prefix "ID_".
     *
     * @param authentication the authentication object
     * @return the user ID
     */
    public static Integer getUserID(Authentication authentication) {
        return Integer.valueOf(
                authentication.getAuthorities().stream().filter(
                        (authority) -> authority.getAuthority().startsWith("ID_"))
                        .toList()
                        .getFirst()
                        .getAuthority()
                        .replace("ID_", ""));
    }

    /**
     * Checks if the media needs an update by determining if the last update was
     * more than one day ago.
     *
     * @param media the media object to check
     * @return <code>true</code> if the media should be updated (i.e., the last
     *         update was more than one day ago
     *         or if the update date is null), <code>false</code> otherwise
     */
    private static Boolean updateMediaWhenLastTimeUpdateMoreThanOneDay(MediaModel media) {
        if (media == null)
            return false;

        LocalDate mediaUpdateAt = media.getUpdateAt() == null ? null : LocalDate.ofInstant(media.getUpdateAt().toInstant(), ZoneId.systemDefault());

        if (mediaUpdateAt == null || ChronoUnit.DAYS.between(mediaUpdateAt, LocalDate.now()) > 1l) {
            return true;
        }

        return false;
    }

    /**
     * Updates all medias in the list when the last time update is more than one
     * day ago. Use the given <code>mediaDataController</code> to update the
     * media.
     * <p>
     *
     * @param medias              the list of medias to update
     * @param mediaDataController the controller to update the media
     * @param typeReference       reference source of information to the Media.
     * @param mediaApi            implementation of MediaAPI.
     * @return the list of updated medias
     */
    public static List<MediaModel> updateGenericMedia(List<MediaModel> medias, MediaDataController mediaDataController,
            TypeReferenceModel typeReference, MediaApi mediaApi) {
        List<MediaModel> updatedMedias = new ArrayList<>();

        for (MediaModel media : medias) {
            updatedMedias.add(updateMediaWhenLastTimeUpdateMoreThanOneDay(media)
                    ? mediaDataController.updateAllInformation(media, null, typeReference, mediaApi)
                    : media);
        }

        return medias;
    }

    /**
     * Updates all medias in the list when the last time update is more than one
     * day ago.
     *
     * @param medias              the list of medias to update
     * @param mediaDataController the controller to update the media
     * @return the list of updated medias
     */
    public static List<MediaModel> updateMedia(List<MediaModel> medias, MediaDataController mediaDataController) {
        List<MediaModel> updatedMedias = new ArrayList<>();

        for (MediaModel media : medias) {
            updatedMedias.add(updateMediaWhenLastTimeUpdateMoreThanOneDay(media)
                    ? mediaDataController.updateAllInformation(media, null)
                    : media);
        }

        return medias;
    }

    /**
     * Returns a map of requested fields where the key is the field name and
     * the value is a list of its subfields.
     * <p>
     * This is useful to select only the fields that are requested by the
     * client, which can be used to reduce the amount of data that needs to
     * be retrieved from the database.
     * <p>
     * For example, if the client requests the following fields:
     *
     * <pre>
     * {
     *   media {
     *     id
     *     title
     *     genres {
     *       name
     *     }
     *   }
     * }
     * </pre>
     *
     * This method will return a map like this:
     *
     * <pre>
     * {
     *   "media": [
     *     "id",
     *     "title",
     *     "genres"
     *   ],
     *   "genres": [
     *     "name"
     *   ]
     * }
     * </pre>
     * <p>
     * This can be used to create a JPA query that only selects the requested
     * fields, which can improve performance.
     * <p>
     *
     * @param environment the {@link DataFetchingEnvironment} that contains
     *                    information about the fields that were requested by
     *                    the client
     * @return a map of requested fields
     */
    public static Map<String, List<String>> getRequestedFields(DataFetchingEnvironment environment) {
        return environment.getSelectionSet().getFields().stream()
                .collect(Collectors.toMap(
                        SelectedField::getName,
                        field -> field.getSelectionSet().getFields().stream()
                                .map(SelectedField::getName)
                                .collect(Collectors.toList()),
                        (existing, replacement) -> {
                            existing.addAll(replacement);
                            return existing;
                        }));
    }

    /**
     * Returns a Pageable object based on the "page" and "size" arguments of the
     * given DataFetchingEnvironment.
     * <p>
     * If the "page" argument is not provided, it defaults to 0. If the "size"
     * argument is not provided, it defaults to 10.
     * <p>
     * The returned Pageable object can be used to construct a JPA query that
     * will return the requested page of data.
     * <p>
     *
     * @param dataFetchingEnvironment the DataFetchingEnvironment that contains
     *                                the "page" and "size" arguments
     * @return a Pageable object
     */
    public static Pageable getPageable(DataFetchingEnvironment dataFetchingEnvironment) {
        int page = dataFetchingEnvironment.getArgumentOrDefault("page", 0);
        int size = dataFetchingEnvironment.getArgumentOrDefault("size", 10);
        Pageable pageable = PageRequest.of(page, size);
        return pageable;
    }
}
