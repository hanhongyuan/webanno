/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 *
 *
 */
public class AnnotationGuideLinePanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private ArrayList<String> documents = new ArrayList<String>();
    private ArrayList<String> selectedDocuments = new ArrayList<String>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private IModel<Project> selectedProjectModel;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AnnotationGuideLinePanel(String id, IModel<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        add(fileUpload = new FileUploadField("content", new Model()));

        add(new Button("importGuideline", new ResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                uploadedFiles = fileUpload.getFileUploads();
                Project project = selectedProjectModel.getObject();

                if (project.getId() == 0) {
                    error("Project not yet created, please save project Details!");
                    return;
                }
                if (isEmpty(uploadedFiles)) {
                    error("No document is selected to upload, please select a document first");
                    return;
                }

                for (FileUpload guidelineFile : uploadedFiles) {

                    try {
                        File tempFile = guidelineFile.writeToTempFile();
                        String fileName = guidelineFile.getClientFileName();
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        projectRepository.createGuideline(project, tempFile, fileName, username);
                    }
                    catch (Exception e) {
                        error("Unable to write guideline file "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

            }
        });

        add(new ListMultipleChoice<String>("documents", new Model(selectedDocuments), documents)
        {
            private static final long serialVersionUID = 1L;

            {
                setChoices(new LoadableDetachableModel<List<String>>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<String> load()
                    {
                        Project project = selectedProjectModel.getObject();
                        documents.clear();
                        if (project.getId() != 0) {
                            documents.addAll(projectRepository.listGuidelines(project));
                        }
                        return documents;
                    }
                });
            }
        });
        
        Button removeGuidelineButton =new Button("remove", new ResourceModel("label"))
        {

            private static final long serialVersionUID = -5021618538109114902L;

            @Override
            public void onSubmit()
            {
                Project project = selectedProjectModel.getObject();
                for (String document : selectedDocuments) {
                    try {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        projectRepository.removeGuideline(project, document, username);
                    }
                    catch (IOException e) {
                        error("Error while removing a document document "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    documents.remove(document);
                }
            }
        };
        
        // Add check to prevent accidental delete operation
        removeGuidelineButton.add(new AttributeModifier("onclick",
                "if(!confirm('Do you really want to delete this Guideline document?')) return false;"));
        
        add(removeGuidelineButton);
 
//        add(new Button("remove", new ResourceModel("label"))
//        {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onSubmit()
//            {
//                Project project = selectedProjectModel.getObject();
//                for (String document : selectedDocuments) {
//                    try {
//                        String username = SecurityContextHolder.getContext().getAuthentication()
//                                .getName();
//                        projectRepository.removeGuideline(project, document, username);
//                    }
//                    catch (IOException e) {
//                        error("Error while removing a document document "
//                                + ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    documents.remove(document);
//                }
//            }
//        });
    }
}
