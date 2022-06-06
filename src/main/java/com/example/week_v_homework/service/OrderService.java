package com.example.week_v_homework.service;

import com.example.week_v_homework.dto.order.OrderDetailRequestDto;
import com.example.week_v_homework.dto.order.OrderDetailResponseDto;
import com.example.week_v_homework.dto.order.OrderRequestDto;
import com.example.week_v_homework.dto.order.OrderResponseDto;
import com.example.week_v_homework.model.Food;
import com.example.week_v_homework.model.Order;
import com.example.week_v_homework.model.OrderDetail;
import com.example.week_v_homework.model.Restaurant;
import com.example.week_v_homework.repository.FoodRepository;
import com.example.week_v_homework.repository.OrderDetailRepository;
import com.example.week_v_homework.repository.OrderRepository;
import com.example.week_v_homework.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public OrderResponseDto orderRequest(OrderRequestDto requestDto) {

        //Order Entity 조립을 위한 재료 확보

        Restaurant restaurant = restaurantRepository.findById(requestDto.getRestaurantId()).orElseThrow(
                () -> new NullPointerException("해당 식당이 존재하지 않습니다")
        );
        int sum = 0;
        System.out.println(requestDto.getFoods());
        for (int i = 0; i < requestDto.getFoods().size(); i++) {
            Food food = foodRepository.findById(requestDto.getFoods().get(i).getId()).orElseThrow(
                    () -> new NullPointerException("해당 음식이 존재하지 않습니다")
            );
            sum += food.getPrice() * requestDto.getFoods().get(i).getQuantity();
        }
        //유효성검사
        if (restaurant.getMinOrderPrice() > sum) {throw new IllegalArgumentException("최소 주문금액 불만족");}
        //Order 조립
        Order order = new Order(restaurant.getName(),restaurant.getDeliveryFee(),sum + restaurant.getDeliveryFee());

        //조립된 order 레코드 저장
        orderRepository.save(order);


        //orderDetail 과 orderDetailResponse 를 함께 조립 시작
        List<OrderDetailResponseDto> foods= new ArrayList<>();

        for (int i = 0; i < requestDto.getFoods().size(); i++) {
            //저장할 레코드 orderDetail 선언 및 필요한 food 레코드를 불러오기
            Food food = foodRepository.findById(requestDto.getFoods().get(i).getId()).orElseThrow(
                    () -> new NullPointerException("해당 음식이 존재하지 않습니다")
            );
            //orderDetail 조립 후 저장
            OrderDetail orderDetail = new OrderDetail(
                    order.getId(),
                    food.getId(),
                    food.getName(),
                    requestDto.getFoods().get(i).getQuantity(),
                    food.getPrice() * requestDto.getFoods().get(i).getQuantity()
            );
            orderDetailRepository.save(orderDetail);

            //orderDetailResponse 조립 시작
            OrderDetailResponseDto responseDto = new OrderDetailResponseDto();
            responseDto.setName(food.getName());
            responseDto.setQuantity(requestDto.getFoods().get(i).getQuantity());
            responseDto.setPrice(food.getPrice() * requestDto.getFoods().get(i).getQuantity());

            //만들어진 orderDetailReponse를 리스트에 추가
            foods.add(responseDto);
        }
        //최종 조립 및 orderResponseDto 반환
        return new OrderResponseDto(
                restaurant.getName(),
                foods,
                restaurant.getDeliveryFee(),
                order.getTotalPrice()
        );
    }

    public List<OrderResponseDto> getOrders () {

        //리턴 타입을 조립하기 위해 빈 배열로 선언
        List<OrderResponseDto> orderResponseDtos = new ArrayList<>();

        //조립 시작 - 조립 재료는 OrderResponseDto
        for (int i = 0; i < orderRepository.findAll().size(); i++) {

            //OrderResponseDto를 조립하기 위해  빈 배열로 선언
            OrderResponseDto orderResponseDto = new OrderResponseDto();

            //조립에 필요한 재료인 order(order들 중 i번째) 불러오기
            Order order = orderRepository.findAll().get(i);

            //order(i번째)값에서  추출한 값을 i번째 orderResponsDto에 삽입
            orderResponseDto.setRestaurantName(order.getRestaurantName());
            orderResponseDto.setDeliveryFee(order.getDeliveryFee());

            //order(i번째)의 orderDetailResponseDtos의 값을 조립하기 위해 빈 배열로 선언
            List<OrderDetailResponseDto> orderDetailResponseDtos = new ArrayList<>();
            int sum = 0;
            //조립 시작 - 조립 재료는 OrderDetailResponseDto
            for (int j = 0; j < orderDetailRepository.findAllByOrderId(order.getId()).size(); j++){

                //조립에 필요한 재료인 OrderDetail(i번째 Order에 바인딩된)들 중 j번째를 불러옴
                OrderDetail orderDetail = orderDetailRepository.findAllByOrderId(order.getId()).get(j);

                // OrderDetailResponseDto에 각 내용을 OrderDetail에서 추출하여 삽입
                OrderDetailResponseDto orderDetailResponseDto = new OrderDetailResponseDto(orderDetail.getFoodName(),orderDetail.getQuantity(),orderDetail.getPrice());

                //orderDetailResponseDtos 조립
                orderDetailResponseDtos.add(orderDetailResponseDto);
                //하는 김에 sum 도 합산 ㅋㅋ
                sum += orderDetail.getPrice();
            }
            //조립된 내용물을 orderResponseDto에 삽입
            orderResponseDto.setFoods(orderDetailResponseDtos);
            //마지막 요소인 totalPrice를 위해 sum과 deliveryFee를 더하여 주입

            orderResponseDto.setTotalPrice(sum + order.getDeliveryFee());
            //orderResponseDtos 조립
            orderResponseDtos.add(orderResponseDto);
        }
        //반환
        return orderResponseDtos;
    }
}
