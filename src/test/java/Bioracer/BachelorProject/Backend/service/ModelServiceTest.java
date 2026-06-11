package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Bioracer.BachelorProject.Backend.controller.DTO.ModelInput;
import Bioracer.BachelorProject.Backend.exception.ModelException;
import Bioracer.BachelorProject.Backend.exception.NotFoundException;
import Bioracer.BachelorProject.Backend.model.Gender;
import Bioracer.BachelorProject.Backend.model.Model;
import Bioracer.BachelorProject.Backend.repository.ModelRepository;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private ModelService modelService;

    private Model existingModel;

    @BeforeEach
    void setUp() {
        existingModel = new Model("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
    }

    @Test
    void getAllModelsReturnsAllModels() {
        List<Model> models = List.of(existingModel);
        when(modelRepository.findAll()).thenReturn(models);

        assertThat(modelService.getAllModels()).containsExactlyElementsOf(models);
    }

    @Test
    void getModelByIdReturnsModel() {
        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        assertThat(modelService.getModelById(5L)).isEqualTo(existingModel);
    }

    @Test
    void getModelByIdThrowsWhenModelDoesNotExist() {
        when(modelRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.getModelById(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Model with ID: 5 not found.");
    }

    @Test
    void createModelStoresModel() {
        ModelInput input = new ModelInput("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        when(modelRepository.existsByName("Gaëlle")).thenReturn(false);
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model created = modelService.createModel(input);

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository).save(modelCaptor.capture());

        Model savedModel = modelCaptor.getValue();
        assertThat(savedModel.getName()).isEqualTo("Gaëlle");
        assertThat(savedModel.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(savedModel.getFront()).isEqualTo("front.jpg");
        assertThat(savedModel.getBack()).isEqualTo("back.jpg");
        assertThat(savedModel.getSide()).isEqualTo("side.jpg");
        assertThat(savedModel.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(created).isEqualTo(savedModel);
    }

    @Test
    void createModelStripsPathsAndUrlsToFilenames() {
        ModelInput input = new ModelInput("Gaëlle",
                "https://example.com/uploads/cover.jpg",
                "uploads/front.jpg",
                "uploads\\back.jpg",
                "/var/images/side.jpg",
                Gender.FEMALE);

        when(modelRepository.existsByName("Gaëlle")).thenReturn(false);
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model created = modelService.createModel(input);

        assertThat(created.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(created.getFront()).isEqualTo("front.jpg");
        assertThat(created.getBack()).isEqualTo("back.jpg");
        assertThat(created.getSide()).isEqualTo("side.jpg");
    }

    @Test
    void createModelThrowsWhenNameIsBlank() {
        ModelInput input = new ModelInput("", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        assertThatThrownBy(() -> modelService.createModel(input))
                .isInstanceOf(ModelException.class)
                .hasMessage("Name is required.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void createModelThrowsWhenNameAlreadyExists() {
        ModelInput input = new ModelInput("Gaëlle", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);

        when(modelRepository.existsByName("Gaëlle")).thenReturn(true);

        assertThatThrownBy(() -> modelService.createModel(input))
                .isInstanceOf(ModelException.class)
                .hasMessage("Model with name: Gaëlle already exists.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void updateModelDetailsUpdatesModel() {
        ModelInput input = new ModelInput("Updated", "new-cover.jpg", "new-front.jpg", "new-back.jpg",
                "new-side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));
        when(modelRepository.save(existingModel)).thenReturn(existingModel);

        Model updated = modelService.updateModelDetails(input, 5L);

        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getCoverImage()).isEqualTo("new-cover.jpg");
        assertThat(updated.getFront()).isEqualTo("new-front.jpg");
        assertThat(updated.getBack()).isEqualTo("new-back.jpg");
        assertThat(updated.getSide()).isEqualTo("new-side.jpg");
        assertThat(updated.getGender()).isEqualTo(Gender.MALE);
    }

    @Test
    void updateModelDetailsKeepsCoverImageWhenInputCoverIsBlank() {
        ModelInput input = new ModelInput("Updated", "", "new-front.jpg", "new-back.jpg",
                "new-side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));
        when(modelRepository.save(existingModel)).thenReturn(existingModel);

        Model updated = modelService.updateModelDetails(input, 5L);

        assertThat(updated.getCoverImage()).isEqualTo("cover.jpg");
    }

    @Test
    void updateModelDetailsThrowsWhenModelDoesNotExist() {
        ModelInput input = new ModelInput("Updated", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.updateModelDetails(input, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Model with ID: 5 does not exist.");
    }

    @Test
    void updateModelDetailsThrowsWhenNameIsBlank() {
        ModelInput input = new ModelInput("", "cover.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        assertThatThrownBy(() -> modelService.updateModelDetails(input, 5L))
                .isInstanceOf(ModelException.class)
                .hasMessage("Name is required.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void updateModelDetailsThrowsWhenFrontIsBlank() {
        ModelInput input = new ModelInput("Updated", "cover.jpg", "", "back.jpg", "side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        assertThatThrownBy(() -> modelService.updateModelDetails(input, 5L))
                .isInstanceOf(ModelException.class)
                .hasMessage("Front image is required.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void updateModelDetailsThrowsWhenBackIsBlank() {
        ModelInput input = new ModelInput("Updated", "cover.jpg", "front.jpg", "", "side.jpg", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        assertThatThrownBy(() -> modelService.updateModelDetails(input, 5L))
                .isInstanceOf(ModelException.class)
                .hasMessage("Back image is required.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void updateModelDetailsThrowsWhenSideIsBlank() {
        ModelInput input = new ModelInput("Updated", "cover.jpg", "front.jpg", "back.jpg", "", Gender.MALE);

        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        assertThatThrownBy(() -> modelService.updateModelDetails(input, 5L))
                .isInstanceOf(ModelException.class)
                .hasMessage("Side image is required.");
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void deleteModelDeletesAllImagesAndModel() {
        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));

        String result = modelService.deleteModel(5L);

        assertThat(result).isEqualTo("Succesfully Deleted");
        verify(uploadService).delete("front.jpg");
        verify(uploadService).delete("back.jpg");
        verify(uploadService).delete("side.jpg");
        verify(uploadService).delete("cover.jpg");
        verify(modelRepository).delete(existingModel);
    }

    @Test
    void deleteModelSkipsCoverImageWhenItEqualsAnotherImage() {
        Model model = new Model("Gaëlle", "front.jpg", "front.jpg", "back.jpg", "side.jpg", Gender.FEMALE);
        when(modelRepository.findById(5L)).thenReturn(Optional.of(model));

        modelService.deleteModel(5L);

        verify(uploadService, times(3)).delete(anyString());
        verify(modelRepository).delete(model);
    }

    @Test
    void deleteModelThrowsWhenModelDoesNotExist() {
        when(modelRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.deleteModel(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Model with ID: 5 does not exist.");
        verify(modelRepository, never()).delete(any(Model.class));
    }

    @Test
    void deleteModelThrowsWhenImageDeleteFails() {
        when(modelRepository.findById(5L)).thenReturn(Optional.of(existingModel));
        doThrow(new RuntimeException("upload server down")).when(uploadService).delete("front.jpg");

        assertThatThrownBy(() -> modelService.deleteModel(5L))
                .isInstanceOf(ModelException.class)
                .hasMessage("Delete failed!");
        verify(modelRepository, never()).delete(any(Model.class));
    }
}
