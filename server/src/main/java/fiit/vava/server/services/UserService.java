package fiit.vava.server.services;

import fiit.vava.server.*;
import fiit.vava.server.config.Constants;
import fiit.vava.server.dao.repositories.ClientRepository;
import fiit.vava.server.dao.repositories.UserRepository;
import io.grpc.stub.StreamObserver;

import java.util.NoSuchElementException;

public class UserService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public UserService() {
        this.userRepository = UserRepository.getInstance();
        this.clientRepository = ClientRepository.getInstance();
    }

    public User authorize(String email, String password) throws SecurityException, NoSuchElementException {
        User user = userRepository.findByEmail(email).orElseThrow(NoSuchElementException::new);

        if (!user.getPassword().equals(password))
            throw new SecurityException();

        return user;
    }

    @Override
    public void me(Empty request, StreamObserver<User> responseObserver) {
        User user = Constants.USER_CONTEXT.get();

        responseObserver.onNext(user);
        responseObserver.onCompleted();
    }

    @Override
    @SkipAuthorization
    public void registerClient(Client request, StreamObserver<Response> responseObserver) {
        try {
            User user = userRepository.save(request.getUser().toBuilder()
                    /*.setStatus(UserState.PENDING)*/.build());

            Client client = clientRepository.save(request.toBuilder()
                    .setUser(user).build());

            Response response = Response.newBuilder()
                    .setUser(user).build();

            responseObserver.onNext(response);
        } catch (Exception ex) {
            Response response = Response.newBuilder()
                    .setError(ex.getMessage()).build();

            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
