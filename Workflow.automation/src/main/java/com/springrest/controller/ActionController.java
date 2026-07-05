package com.springrest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.Action;
import com.springrest.repository.ActionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    @Autowired
    private ActionRepository actionRepository;

    @GetMapping
    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    @GetMapping("/{id}")
    public Action getActionById(@PathVariable Long id) {
        return actionRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public Action createAction(@RequestBody Action action) {
    	System.out.println("action Object "+ action.toString());
        return actionRepository.save(action);
    }

    @PutMapping("/{id}")
    public Action updateAction(@PathVariable Long id, @RequestBody Action action) {
        action.setId(id);
        return actionRepository.save(action);
    }

    @DeleteMapping("/{id}")
    public void deleteAction(@PathVariable Long id) {
        actionRepository.deleteById(id);
    }
}
