package Bioracer.BachelorProject.Backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import Bioracer.BachelorProject.Backend.model.Project;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;

class ProjectRepositoryPersistenceTest {

        @Test
        void galleryIsConfiguredAsPersistentElementCollection() throws NoSuchFieldException {
                Field imagesField = Project.class.getDeclaredField("images");

                ElementCollection elementCollection = imagesField.getAnnotation(ElementCollection.class);
                CollectionTable collectionTable = imagesField.getAnnotation(CollectionTable.class);
                OrderColumn orderColumn = imagesField.getAnnotation(OrderColumn.class);

                assertThat(elementCollection).isNotNull();
                assertThat(collectionTable).isNotNull();
                assertThat(collectionTable.name()).isEqualTo("project_images");
                assertThat(orderColumn).isNotNull();
        }
}
