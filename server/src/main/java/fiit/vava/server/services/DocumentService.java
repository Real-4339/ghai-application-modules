package fiit.vava.server.services;

import com.google.protobuf.ByteString;
import fiit.vava.server.*;
import fiit.vava.server.config.Constants;
import fiit.vava.server.dao.repositories.document.DocumentRepository;
import fiit.vava.server.dao.repositories.document.field.DocumentFieldRepository;
import fiit.vava.server.dao.repositories.document.request.DocumentRequestRepository;
import fiit.vava.server.dao.repositories.document.template.DocumentTemplateRepository;
import fiit.vava.server.dao.repositories.document.template.fields.DocumentTemplateFieldRepository;
import fiit.vava.server.dao.repositories.user.client.ClientRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DocumentService extends DocumentServiceGrpc.DocumentServiceImplBase {

    DocumentRepository documentRepository;
    DocumentTemplateRepository documentTemplateRepository;
    DocumentTemplateFieldRepository documentTemplateFieldRepository;
    DocumentRequestRepository documentRequestRepository;
    DocumentFieldRepository documentFieldRepository;
    ClientRepository clientRepository;

    private final String PATH_TO_SAVE_FILES;

    public DocumentService() {
        this.documentRepository = DocumentRepository.getInstance();
        this.documentTemplateRepository = DocumentTemplateRepository.getInstance();
        this.documentTemplateFieldRepository = DocumentTemplateFieldRepository.getInstance();
        this.documentRequestRepository = DocumentRequestRepository.getInstance();
        this.documentFieldRepository = DocumentFieldRepository.getInstance();
        this.clientRepository = ClientRepository.getInstance();

        Dotenv dotenv = Dotenv.load();
        this.PATH_TO_SAVE_FILES = dotenv.get("PATH_TO_SAVE_FILES");
    }

    private String saveFile(String fileName, byte[] data) throws IOException {
        Path path = Paths.get(PATH_TO_SAVE_FILES, fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, data, StandardOpenOption.CREATE);
        return path.toString();
    }

    @Override
    public void getFileByPath(GetFileByPathRequest request, StreamObserver<GetFileByPathResponse> responseObserver) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(request.getPath()));
            responseObserver.onNext(
                    GetFileByPathResponse.newBuilder()
                            .setFile(ByteString.copyFrom(data))
                            .build()
            );
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read a file from local file system.");
        }
    }

    @Override
    public void createDocumentTemplate(CreateDocumentTemplateRequest request, StreamObserver<DocumentTemplate> responseObserver) {
        User user = Constants.USER_CONTEXT.get();

        String path;
        try {
            path = saveFile(
                    user.getEmail() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
                    request.getFile().toByteArray()
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to save a file to local file system.");
        }

        DocumentTemplate documentTemplate = documentTemplateRepository.save(DocumentTemplate.newBuilder()
                .setName(request.getName())
                .setType(request.getType())
                .setPath(path)
                .build());

        System.out.println("Document template created: " + documentTemplate.getId());

        request.getFieldsList().forEach(field ->
                documentTemplateFieldRepository.save(DocumentTemplateField.newBuilder()
                        .setTemplate(documentTemplate)
                        .setName(field.getName())
                        .setType(field.getType())
                        .setRequired(field.getRequired())
                        .build()));

        responseObserver.onNext(documentTemplate);
        responseObserver.onCompleted();
    }

    @Override
    public void getDocumentTemplateById(GetDocumentTemplateByIdRequest request, StreamObserver<DocumentTemplateWithFields> responseObserver) {
        DocumentTemplate documentTemplate = documentTemplateRepository.findById(request.getId());

        if (documentTemplate == null)
            throw new RuntimeException("Document template not found.");

        DocumentTemplateWithFields response = DocumentTemplateWithFields.newBuilder()
                .setTemplate(documentTemplate)
                .addAllFields(documentTemplateFieldRepository.findAllByDocumentTemplate(documentTemplate))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllDocumentTemplates(Empty request, StreamObserver<GetAllDocumentTemplates> responseObserver) {
        responseObserver.onNext(GetAllDocumentTemplates.newBuilder()
                .addAllTemplates(documentTemplateRepository.findAll())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createDocumentRequest(CreateDocumentRequest request, StreamObserver<DocumentRequest> responseObserver) {
        User user = Constants.USER_CONTEXT.get();

        Client client = clientRepository.findByUserId(user.getId());

        DocumentTemplate template = documentTemplateRepository.findById(request.getTemplateId());

        if (template == null)
            throw new RuntimeException("document template not found.");

        DocumentRequestStatus status = DocumentRequestStatus.CREATED;

        DocumentRequest documentRequest = documentRequestRepository.save(
                DocumentRequest.newBuilder()
                        .setClient(client)
                        .setTemplate(template)
                        .setStatus(status)
                        .build());

        request.getFieldsList().forEach(field ->
                documentFieldRepository.save(field.toBuilder()
                        .setRequest(documentRequest)
                        .build()
                ));

        responseObserver.onNext(documentRequest);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllMineDocumentRequests(Empty request, StreamObserver<GetAllDocumentRequestsResponse> responseObserver) {
        User user = Constants.USER_CONTEXT.get();

        Client client = clientRepository.findByUserId(user.getId());

        List<DocumentRequest> documentRequests = documentRequestRepository.findAllByClientId(client.getId());

        GetAllDocumentRequestsResponse response = GetAllDocumentRequestsResponse.newBuilder()
                .addAllDocumentRequests(documentRequests)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
