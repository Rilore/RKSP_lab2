package ru.avt913.clientserverlab2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.avt913.clientserverlab2.dto.ProductBetweenDto;
import ru.avt913.clientserverlab2.dto.ProductGetDto;
import ru.avt913.clientserverlab2.dto.SalesGetDto;
import ru.avt913.clientserverlab2.entity.Product;
import ru.avt913.clientserverlab2.mapper.ProductMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@Tag(name = "Лабораторная работа №2", description = "Работа с товарами")
public class ProductController {

    private final JdbcTemplate jdbcTemplate;
    private final ProductMapper productMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProductController(ProductMapper productMapper, JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.productMapper = productMapper;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping
    @Operation(summary = "Первый запрос", description = "Получение всех товаров")
    public ResponseEntity<List<ProductGetDto>> getAllProducts() {
        List<Product> products = jdbcTemplate.query("SELECT * FROM products", new BeanPropertyRowMapper<>(Product.class));

        return ResponseEntity.ok(products.stream().map(productMapper::productToProductGetDto).collect(Collectors.toList()));
    }

    @GetMapping("/category")
    @Operation(summary = "Второй запрос", description = "Получение всех товаров по заданной категории")
    public ResponseEntity<List<ProductGetDto>> getAllProductsByCategory(
            @RequestParam("name") @Parameter(description = "Категория товара") String category) {
        List<Product> products = jdbcTemplate.query("SELECT * FROM products WHERE category = ?", new BeanPropertyRowMapper<>(Product.class), category);

        return ResponseEntity.ok(products.stream().map(productMapper::productToProductGetDto).collect(Collectors.toList()));
    }

    @GetMapping("/between")
    @Operation(summary = "Третий запрос", description = "Получение продаж товара в заданный промежуток времени")
    public ResponseEntity<List<ProductBetweenDto>> getAllProductBetweenOrderDate(
            @RequestParam("from") @Parameter(description = "Начало промежутка") Instant from, @RequestParam("to") @Parameter(description = "Конец промежутка") Instant to,
            @RequestParam(value = "name", required = false) @Parameter(description = "Название товара") String name, @RequestParam(required = false)
            @Parameter(description = "Сортировка") String sort) {        
                StringBuilder sql = new StringBuilder("select product_id, product_code, product_name, category, standard_cost, order_date\n" +
                "from products\n" +
                "         inner join order_details od on products.id = od.product_id\n" +
                "         inner join orders o on od.order_id = o.id\n" +
                "where order_date between ? and ?\n");
        if (name != null) {
            sql.append("AND product_name LIKE '%").append(name).append("%'\n");
        }
        sql.append("ORDER BY order_date");

        if (Objects.equals(sort, "desc")) {
            sql.append(" DESC");
        }

        List<ProductBetweenDto> products = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(ProductBetweenDto.class), from, to);
        return ResponseEntity.ok(products);
    }

    @GetMapping("orders")
    @Operation(summary = "Четвертый запрос", description = "Получение продаж для определенного товара по месяцам за указанный год")
    ResponseEntity<?> getOrdersWithMonths(@RequestParam("name") @Parameter(description = "Название товара") String name,
                                          @RequestParam("year") @Parameter(description = "Год") Integer year) {
        String query = "select month(o.order_date) as month, \n" + "    p.id as id, \n"
                + "    p.product_code as product_code, \n" + "    p.product_name as product_name,  \n"
                + "    p.category, \n" + "    count(*) as count \n" + "from order_details \n" + "    join"
                + " orders o on o.id = order_details.order_id \n"
                + "    join products p on order_details.product_id = p.id\n"
                + "where product_name = :productName\n" + "and year(o.order_date) = :year\n"
                + "group by month(o.order_date), p.id, p.product_code, p.product_name, p.category";

        List<SalesGetDto> ordersWithMonthsList = namedParameterJdbcTemplate.query(query, new MapSqlParameterSource("productName", name).addValue("year", year), new BeanPropertyRowMapper<>(SalesGetDto.class));

        return ResponseEntity.ok(ordersWithMonthsList);
    }
}