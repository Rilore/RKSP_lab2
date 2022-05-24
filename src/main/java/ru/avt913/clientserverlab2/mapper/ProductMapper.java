package ru.avt913.clientserverlab2.mapper;

import org.mapstruct.Mapper;
import ru.avt913.clientserverlab2.dto.ProductGetDto;
import ru.avt913.clientserverlab2.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductGetDto productToProductGetDto(Product product);
}
