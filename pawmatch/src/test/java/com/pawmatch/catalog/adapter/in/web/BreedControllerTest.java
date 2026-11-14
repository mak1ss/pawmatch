package com.pawmatch.catalog.adapter.in.web;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.pawmatch.catalog.application.port.in.GetBreedImageUseCase;
import com.pawmatch.catalog.application.port.in.GetBreedUseCase;
import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.BreedImage;
import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BreedController.class)
class BreedControllerTest {

    private static final byte[] IMAGE_BYTES = "abc".getBytes();
    private static final String IMAGE_ETAG = "\"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad\"";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListBreedsUseCase listBreeds;
    @MockitoBean
    GetBreedUseCase getBreed;
    @MockitoBean
    GetBreedImageUseCase getBreedImage;

    @Test
    void listsBreedsInSnakeCaseWithImageUrls() throws Exception {
        given(listBreeds.listBreeds()).willReturn(List.of(TestBreeds.labradorWithImage()));

        mockMvc.perform(get("/api/v1/breeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TestBreeds.LABRADOR_ID.toString()))
                .andExpect(jsonPath("$[0].slug").value("labrador-retriever"))
                .andExpect(jsonPath("$[0].name_uk").value("Лабрадор-ретривер"))
                .andExpect(jsonPath("$[0].size").value("Large"))
                .andExpect(jsonPath("$[0].energy_level").value(5))
                .andExpect(jsonPath("$[0].image_url").value("http://localhost/api/v1/breeds/labrador-retriever/image"));
    }

    @Test
    void breedWithoutImageHasNullImageUrl() throws Exception {
        given(listBreeds.listBreeds()).willReturn(List.of(TestBreeds.labrador()));

        mockMvc.perform(get("/api/v1/breeds"))
                .andExpect(jsonPath("$[0].image_url").value(nullValue()));
    }

    @Test
    void detailSendsUnknownValuesAsExplicitNulls() throws Exception {
        given(getBreed.getBreed("labrador-retriever")).willReturn(TestBreeds.labrador());

        mockMvc.perform(get("/api/v1/breeds/labrador-retriever"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scores.watchdog_protective_nature").value(3))
                .andExpect(jsonPath("$.coat_length[0]").value("Short"))
                .andExpect(jsonPath("$.source_url").value("https://www.akc.org/dog-breeds/labrador-retriever/"))
                .andExpect(jsonPath("$.alone_tolerance_hours").value(nullValue()))
                .andExpect(jsonPath("$", hasKey("alone_tolerance_hours")))
                .andExpect(jsonPath("$", hasKey("good_with_cats")))
                .andExpect(jsonPath("$", hasKey("image_attribution")))
                .andExpect(jsonPath("$.image_attribution").value(nullValue()));
    }

    @Test
    void detailIncludesImageAttribution() throws Exception {
        given(getBreed.getBreed("labrador-retriever")).willReturn(TestBreeds.labradorWithImage());

        mockMvc.perform(get("/api/v1/breeds/labrador-retriever"))
                .andExpect(jsonPath("$.image_attribution.author").value("Jane Doe"))
                .andExpect(jsonPath("$.image_attribution.license").value("CC BY-SA 4.0"));
    }

    @Test
    void unknownBreedIs404() throws Exception {
        given(getBreed.getBreed("unknown")).willThrow(new ResourceNotFoundException("breed", "unknown"));

        mockMvc.perform(get("/api/v1/breeds/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("breed"));
    }

    @Test
    void imageIsServedWithContentTypeEtagAndCacheControl() throws Exception {
        given(getBreedImage.getBreedImage("labrador-retriever"))
                .willReturn(new BreedImage(TestBreeds.image("labrador-retriever.webp"), IMAGE_BYTES));

        mockMvc.perform(get("/api/v1/breeds/labrador-retriever/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/webp"))
                .andExpect(header().string("ETag", IMAGE_ETAG))
                .andExpect(header().string("Cache-Control", "max-age=86400, public"))
                .andExpect(content().bytes(IMAGE_BYTES));
    }

    @Test
    void matchingIfNoneMatchReturns304() throws Exception {
        given(getBreedImage.getBreedImage("labrador-retriever"))
                .willReturn(new BreedImage(TestBreeds.image("labrador-retriever.webp"), IMAGE_BYTES));

        mockMvc.perform(get("/api/v1/breeds/labrador-retriever/image").header("If-None-Match", IMAGE_ETAG))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void missingImageIs404() throws Exception {
        given(getBreedImage.getBreedImage("labrador-retriever"))
                .willThrow(new ResourceNotFoundException("breed_image", "labrador-retriever"));

        mockMvc.perform(get("/api/v1/breeds/labrador-retriever/image"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resource").value("breed_image"));
    }
}
