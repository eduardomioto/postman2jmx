package com.loadium.postman2jmx.builder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.loadium.postman2jmx.config.Postman2JmxConfig;
import com.loadium.postman2jmx.exception.NoPostmanCollectionItemException;
import com.loadium.postman2jmx.exception.NullPostmanCollectionException;
import com.loadium.postman2jmx.model.jmx.JmxFile;
import com.loadium.postman2jmx.model.jmx.JmxHeaderManager;
import com.loadium.postman2jmx.model.jmx.JmxJsonPostProcessor;
import com.loadium.postman2jmx.model.jmx.JmxLoopController;
import com.loadium.postman2jmx.model.jmx.JmxTestPlan;
import com.loadium.postman2jmx.model.jmx.JmxThreadGroup;
import com.loadium.postman2jmx.model.postman.PostmanCollection;
import com.loadium.postman2jmx.model.postman.PostmanItem;

/**
 * The Class AbstractJmxFileBuilder.
 */
public abstract class AbstractJmxFileBuilder implements IJmxFileBuilder {

    private static Logger logger = LoggerFactory.getLogger(AbstractJmxFileBuilder.class.getName());

    /**
     * Builds the jmx file.
     *
     * @param postmanCollection the postman collection
     * @param jmxOutputFilePath the jmx output file path
     * @return the jmx file
     * @throws Exception the exception
     */
    protected JmxFile buildJmxFile(PostmanCollection postmanCollection, String jmxOutputFilePath) throws Exception {
        if (postmanCollection == null) {
            throw new NullPostmanCollectionException();
        }

        if (postmanCollection.getItems() == null || postmanCollection.getItems().isEmpty()) {
            throw new NoPostmanCollectionItemException();
        }

        final Postman2JmxConfig config = new Postman2JmxConfig();
        config.setJMeterHome();

        // TestPlan
        final TestPlan testPlan = JmxTestPlan.newInstance(postmanCollection.getInfo() != null ? postmanCollection.getInfo().getName() : "");

        // ThreadGroup controller
        final LoopController loopController = JmxLoopController.newInstance();

        // ThreadGroup
        final ThreadGroup threadGroup = JmxThreadGroup.newInstance(loopController);

        // HTTPSamplerProxy
        final List<HTTPSamplerProxy> httpSamplerProxies = new ArrayList<>();
        final List<HeaderManager> headerManagers = new ArrayList<>();

        for (final PostmanItem item : postmanCollection.getItems()) {

            final IJmxBodyBuilder bodyBuilder = JmxBodyBuilderFactory.getJmxBodyBuilder(item);
            final HTTPSamplerProxy httpSamplerProxy = bodyBuilder.buildJmxBody(item);
            httpSamplerProxies.add(httpSamplerProxy);

            headerManagers.add(JmxHeaderManager.newInstance(item.getName(), item.getRequest().getHeaders()));
        }

        for (final HeaderManager headerManager : headerManagers) {
            logger.info("Header Manager Name: {}", headerManager.getName());
        }

        // Create TestPlan hash tree
        final HashTree testPlanHashTree = new HashTree();
        testPlanHashTree.add(testPlan);

        // Add ThreadGroup to TestPlan hash tree
        HashTree threadGroupHashTree = new HashTree();
        threadGroupHashTree = testPlanHashTree.add(testPlan, threadGroup);

        // Add Http Sampler to ThreadGroup hash tree
        HashTree httpSamplerHashTree = new HashTree();

        // Add header manager hash tree
        HashTree headerHashTree = null;

        // Add Java Sampler to ThreadGroup hash tree
        for (int i = 0; i < httpSamplerProxies.size(); i++) {
            final HTTPSamplerProxy httpSamplerProxy = httpSamplerProxies.get(i);
            final HeaderManager headerManager = headerManagers.get(i);

            logger.info("Java Sampler : {}", headerManager.getName());

            httpSamplerHashTree = threadGroupHashTree.add(httpSamplerProxy);

            headerHashTree = new HashTree();
            headerHashTree = httpSamplerHashTree.add(headerManager);

            final PostmanItem postmanItem = postmanCollection.getItems().get(i);
            if (!postmanItem.getEvents().isEmpty()) {
                final List<JSONPostProcessor> jsonPostProcessors = JmxJsonPostProcessor.getJsonPostProcessors(postmanItem);
                httpSamplerHashTree.add(jsonPostProcessors);
            }
        }

        final JmxFile jmxFile = saveJMXFile(jmxOutputFilePath, testPlanHashTree);

        return jmxFile;
    }

    /**
     * Save JMX file.
     *
     * @param jmxOutputFilePath the jmx output file path
     * @param testPlanHashTree the test plan hash tree
     * @return the jmx file
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private JmxFile saveJMXFile(String jmxOutputFilePath, final HashTree testPlanHashTree) throws FileNotFoundException, IOException {
        final File file = new File(jmxOutputFilePath);
        final OutputStream os = new FileOutputStream(file);
        SaveService.saveTree(testPlanHashTree, os);

        final InputStream is = new FileInputStream(file);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];

        for (int len = 0; (len = is.read(buffer)) != -1; ) {
            bos.write(buffer, 0, len);
        }
        bos.flush();

        final byte[] data = bos.toByteArray();
        final JmxFile jmxFile = new JmxFile(data, testPlanHashTree);

        os.close();
        is.close();
        bos.close();
        return jmxFile;
    }

}
