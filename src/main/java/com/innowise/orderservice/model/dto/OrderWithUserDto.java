package com.innowise.orderservice.model.dto;

import com.innowise.commonstarter.model.dto.UserDto;

public record OrderWithUserDto(
    OrderDto order,
    UserDto user
) {

}