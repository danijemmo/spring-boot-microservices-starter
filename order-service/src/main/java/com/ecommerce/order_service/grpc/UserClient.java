package com.ecommerce.order_service.grpc;

import com.ecommerce.grpc.user.UserRequest;
import com.ecommerce.grpc.user.UserResponse;
import com.ecommerce.grpc.user.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class UserClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserResponse getUser(String userId) {
        return userStub.getUser(
                UserRequest.newBuilder().setUserId(userId).build()
        );
    }
}
