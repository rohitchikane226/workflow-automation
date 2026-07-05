package com.springrest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.Trigger;
import com.springrest.Entities.TriggerField;
import com.springrest.repository.TriggerRepository;
import com.springrest.repository.TriggerFieldRepository;

import java.util.List;

@RestController
@RequestMapping("/api/triggers")
public class TriggerController {

    @Autowired
    private TriggerRepository triggerRepository;
    @Autowired
    private TriggerFieldRepository fieldRepo;

    @GetMapping
    public List<Trigger> getAllTriggers() {
        return triggerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Trigger getTriggerById(@PathVariable Long id) {
        return triggerRepository.findById(id).orElseThrow();
    }

    @PostMapping
    public Trigger createTrigger(@RequestBody Trigger trigger) {
        return triggerRepository.save(trigger);
    }

    @PutMapping("/{id}")
    public Trigger updateTrigger(@PathVariable Long id, @RequestBody Trigger trigger) {
        trigger.setId(id);
        return triggerRepository.save(trigger);
    }

    @DeleteMapping("/{id}")
    public void deleteTrigger(@PathVariable Long id) {
        triggerRepository.deleteById(id);
    }
//    @GetMapping("/{id}/fields")
//    public List<TriggerField> getTriggerFields(@PathVariable Long id) {
//        return fieldRepo.findByTriggerId(id);
//    }
}
