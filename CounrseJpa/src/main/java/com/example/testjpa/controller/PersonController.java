package com.example.testjpa.controller;


import com.example.testjpa.dao.PersonDao;
import com.example.testjpa.dto.PeronWithMeterOnly;
import com.example.testjpa.dto.PersonDto;
import com.example.testjpa.model.Meter;
import com.example.testjpa.model.Person;
import com.example.testjpa.model.Pet;
import com.example.testjpa.model.QPerson;
import com.querydsl.core.types.Predicate;
import org.hibernate.query.criteria.internal.expression.function.LowerFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

/**
 * Created by cloud on 2019/9/28.
 */
@RequestMapping("/p")
@RestController
public class PersonController {
    @Autowired
    PersonDao personDao;
    @Resource
    EntityManager entityManager;

    @RequestMapping("/add")
    @ResponseBody
    public Object add() {
        Person person = new Person();
        person.setDeleteMark(0);
        person.setAge(18);
        person.setName("周");

        List<Meter> meterList = new ArrayList<>();
        Meter meter = new Meter();
        meter.setCode("1");
        meter.setType("D");
        meter.setPerson(person);
        meterList.add(meter);

        List<Pet> pets = new ArrayList<>();
        Pet pet = new Pet();
        pet.setName("狗狗");
        pets.add(pet);

        person.setPets(pets);
        person.setMeters(meterList);

        personDao.save(person);
        return "ok";
    }

    @RequestMapping("/delete")
    public Object delete(Integer id) {
        Person person = personDao.findById(id).orElse(null);
        if (person != null) {
            personDao.delete(person);
        }
        return "ok";
    }

    @RequestMapping("/update")
    public Object update(Integer id) {
        Person person = personDao.findById(id).get();
        person.setName("qq");
        personDao.saveAndFlush(person);
        person.setName("tt");
        personDao.save(person);
       
        return "ok";
    }

    @RequestMapping("/{id}")
    public Object showUserForm(@PathVariable("id") Person person) {
        //@EnableSpringDataWebSupport 支持 自动根据传入Id 查询person
        // https://docs.spring.io/spring-data/data-jpa/docs/current/reference/html/#core.web.basic
        return person;
    }


    @RequestMapping("/list")
    public Object list() {
        //Example查询 包括匹配模式
        //https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#query-by-example.matchers
        Person searchPerson = new Person();
        searchPerson.setName("jack");
        searchPerson.setIdCard("510200X");
        Example<Person> personExample = Example.of(searchPerson,
                ExampleMatcher.matching().withMatcher("name", contains().ignoreCase())
        );
        return personDao.findAll(personExample);
    }


    @RequestMapping("/list2")
    public Object list3(@QuerydslPredicate(root = Person.class) Predicate predicate) {
        //QDSL WEB支持
        //https://docs.spring.io/spring-data/data-jpa/docs/current/reference/html/#core.web.type-safe
        QPerson p = QPerson.person;
        personDao.findAll(p.id.eq(1));

        return personDao.findAll(predicate);
    }


    @RequestMapping("/list3")
    public Object list4() {
        //动态预测映射
        //https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projection.dynamic
        Collection<PersonDto> ps = personDao.findAllDtoedBy();
        return ps;
    }

    @RequestMapping("/list4")
    public Object list3() {
        //动态预测映射
        //https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projection.dynamic
        Sort sort = Sort.by(Sort.Order.desc("meters.id"));
        Collection<PeronWithMeterOnly> ps = personDao.findAllPeronWithMeterOnlyOrderBy(sort);
        return ps;
    }

    @RequestMapping("/list5")
    public Object list5() {
        // HQL
        // https://docs.jboss.org/hibernate/orm/5.3/userguide/html_single/Hibernate_User_Guide.html#hql
        List<Person> p = entityManager.createQuery(" select DISTINCT p from Person p left join fetch p.pets ").getResultList();
        return p;
    }

    @RequestMapping("/list6")
    public Object list6() {
        // CriteriaQuery
        // https://docs.jboss.org/hibernate/orm/5.3/userguide/html_single/Hibernate_User_Guide.html#criteria
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Person> query = builder.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);
        query.select(root).where(
                builder.or(
                        builder.equal(root.get("id"), 12),
                        builder.equal(root.get("id"), 18),
                        builder.equal(builder.function(LowerFunction.NAME, String.class, root.get("name")), "周蜀S".toLowerCase())
                )
        );

        List a = entityManager.createQuery(query).getResultList().stream().map(person -> {
            person.setPets(null); //排除关联对象 避免在json序列化是 触发get导致进行懒加载
            person.setMeters(null);
            return person;
        }).collect(Collectors.toList());

        /*CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        countQuery.select(builder.count(countQuery.from(Person.class)));*/

        return a;
    }

    @RequestMapping("/list7")
    public Object list7() {
        // CriteriaQuery  查询部分字段
        // https://docs.jboss.org/hibernate/orm/5.3/userguide/html_single/Hibernate_User_Guide.html#criteria
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<Person> root = query.from(Person.class);
        query.select(builder.array(root.get("id"), root.get("name")));

        return entityManager.createQuery(query).getResultList();
    }

    @RequestMapping("/list8")
    public Object list8() {
        // CriteriaQuery 通过构造器 映射DTO对象
        // https://docs.jboss.org/hibernate/orm/5.3/userguide/html_single/Hibernate_User_Guide.html#criteria
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PersonDto> query = builder.createQuery(PersonDto.class);
        Root<Person> root = query.from(Person.class);
        query.select(builder.construct(PersonDto.class, root.get("name"), root.get("age")));
        return entityManager.createQuery(query).getResultList();
    }


    @RequestMapping("/procedure")
    public Object callPlus1InOut(Pageable first) {
        Map a = personDao.plus1(1);
        return a;
    }


    @RequestMapping("/test")
    public Object test(Person person) {

        person.setDeleteMark(0);

        return "ok";
    }

}