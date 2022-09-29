package org.springframework.sync.diffsync.web;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.sync.TodoRepository;
import org.springframework.sync.diffsync.EmbeddedDataSourceConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes= EmbeddedDataSourceConfig.class)
@Transactional
public class DiffSyncWsControllerTest {

    private static final String RESOURCE_PATH = "/todos";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TodoRepository repository;

    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");

    //
    // private helpers
    //

    private String resource(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("/org/springframework/sync/" + name + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        StringBuilder builder = new StringBuilder();
        while(reader.ready()) {
            builder.append(reader.readLine());
        }
        return builder.toString();
    }

    private TodoRepository todoRepository() {
        return repository;
    }
}
