package com.espacogeek.geek;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.CompanyModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.MediaStatusModel;
import com.espacogeek.geek.models.PeopleModel;
import com.espacogeek.geek.models.SeasonModel;
import com.espacogeek.geek.models.StatusType;
import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.types.AuthPayload;
import com.espacogeek.geek.types.BatchJobExecution;
import com.espacogeek.geek.types.BatchJobPage;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;
import com.espacogeek.geek.types.NewUser;
import com.espacogeek.geek.types.QuoteArtwork;
import com.espacogeek.geek.types.UpdateUserMediaInput;

@SpringBootApplication(exclude = {
		JmxAutoConfiguration.class,
		FreeMarkerAutoConfiguration.class,
		ThymeleafAutoConfiguration.class,
		MustacheAutoConfiguration.class,
		GsonAutoConfiguration.class,
		H2ConsoleAutoConfiguration.class,
		WebSocketServletAutoConfiguration.class,
})
@RegisterReflectionForBinding({
		// GraphQL response types / DTOs
		MediaPage.class,
		MediaSimplefied.class,
		AuthPayload.class,
		QuoteArtwork.class,
		BatchJobPage.class,
		BatchJobExecution.class,
		// Input types
		NewUser.class,
		UpdateUserMediaInput.class,
		// JPA models returned via GraphQL
		MediaModel.class,
		UserModel.class,
		UserMediaListModel.class,
		UserCustomStatusModel.class,
		SeasonModel.class,
		GenreModel.class,
		CompanyModel.class,
		PeopleModel.class,
		ExternalReferenceModel.class,
		AlternativeTitleModel.class,
		MediaCategoryModel.class,
		MediaStatusModel.class,
		// Enums used in GraphQL schema
		StatusType.class,
		CategoryType.class,
})
public class GeekApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeekApplication.class, args);
	}

}
