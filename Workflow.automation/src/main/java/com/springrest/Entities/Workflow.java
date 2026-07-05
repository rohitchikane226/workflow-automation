package com.springrest.Entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        
    private String description;
    

    private boolean active;    

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<WorkflowStep> steps;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private boolean locked = false;
    private LocalDateTime nextPollTime;
    public Workflow() {}

    public Workflow(String name, String description, List<WorkflowStep> steps) {
        this.name = name;
        this.description = description;
        this.steps = steps;
    }

    public LocalDateTime getNextPollTime() {
	    return nextPollTime;
	}

	public void setNextPollTime(LocalDateTime nextPollTime) {
	    this.nextPollTime = nextPollTime;
	}
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public boolean isLocked() {
        return locked;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps;
    }
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
