package com.ecommerce.user_service.grpc;

import com.ecommerce.user_service.entities.User;
import com.ecommerce.user_service.repositories.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import com.ecommerce.grpc.user.UserServiceGrpc;
import com.ecommerce.grpc.user.UserRequest;
import com.ecommerce.grpc.user.UserResponse;

import java.util.UUID;

@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userRepository.findById(UUID.fromString(request.getUserId()))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserResponse response = UserResponse.newBuilder()
                    .setId(user.getId().toString())
                    .setFirstName(user.getFirstName())
                    .setLastName(user.getLastName())
                    .setEmail(user.getEmail())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (RuntimeException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
