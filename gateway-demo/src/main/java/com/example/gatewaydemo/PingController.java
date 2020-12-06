package com.example.gatewaydemo;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ping")
@Slf4j
public class PingController {


    @Autowired
    private InMemoryRouteDefinitionRepository memoryRouteDefinitionRepository;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private RouteLocator routeLocator;

    @RequestMapping("route")
    public Mono<List<Map<String, Object>>> getAllRoutesList() {
        Mono<Map<String, RouteDefinition>> routeDefs = routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId).map(t-> {
                    log.info("---------->get RouteDefinition Repository>" + JSON.toJSONString(t));
                    return t;});
        Mono<List<Route>> routes = this.routeLocator.getRoutes().collectList().map(t-> {
            log.info("---------->get All Repository>" + JSON.toJSONString(t));
            return t;});;

        return Mono.zip(routeDefs, routes).map(tuple -> {
            Map<String, RouteDefinition> defs = tuple.getT1();
            List<Route> routeList = tuple.getT2();
            List<Map<String, Object>> allRoutes = new ArrayList<>();

            routeList.forEach(route -> {
                HashMap<String, Object> r = new HashMap<>();
                r.put("route_id", route.getId());
                r.put("order", route.getOrder());
                if (defs.containsKey(route.getId())) {
                    r.put("route_definition", defs.get(route.getId()));
                } else {
                    HashMap<String, Object> obj = new HashMap<>();
                    obj.put("predicate", route.getPredicate().toString());
                    if (!route.getFilters().isEmpty()) {
                        ArrayList<String> filters = new ArrayList<>();
                        for (GatewayFilter filter : route.getFilters()) {
                            filters.add(filter.toString());
                        }
                        obj.put("filters", filters);
                    }
                    if (!obj.isEmpty()) {
                        r.put("route_object", obj);
                    }
                }
                allRoutes.add(r);
            });

            return allRoutes;
        });
    }
}
