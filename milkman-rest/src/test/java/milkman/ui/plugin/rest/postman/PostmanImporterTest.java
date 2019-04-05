package milkman.ui.plugin.rest.postman;


import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import milkman.domain.Collection;
import milkman.domain.Environment;

class PostmanImporterTest {

	@Test
	void shouldConvertCollectionProperly() throws IOException, Exception {
		PostmanImporter sut = new PostmanImporter();
		Collection collection  = sut.importCollection(IOUtils.toString(getClass().getResourceAsStream("/test.postman_collection.json"), "UTF-8"));
		assertThat(collection.getName()).isEqualTo("test");
	}

	@Test
	void shouldConvertEnvironmentProperly() throws IOException, Exception {
		PostmanImporter sut = new PostmanImporter();
		Environment env = sut.importEnvironment(IOUtils.toString(getClass().getResourceAsStream("/test.postman_environment.json"), "UTF-8"));
		assertThat(env.getName()).isEqualTo("test");
	}
}
