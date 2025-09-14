//package ru.invest.agent.component;
//
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DatabaseHealthCheck implements HealthIndicator {
//
//    private final MongoTemplate mongoTemplate;
//
//    public DatabaseHealthCheck(MongoTemplate mongoTemplate) {
//        this.mongoTemplate = mongoTemplate;
//    }
//
//    @Override
//    public Health health() {
//        try {
//            mongoTemplate.executeCommand("{ ping: 1 }");
//            return Health.up().build();
//        } catch (Exception e) {
//            return Health.down()
//                    .withDetail("error", e.getMessage())
//                    .build();
//        }
//    }
//}