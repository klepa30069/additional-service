package ru.hpclab.hl.additional.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.io.Serializable;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="visitor")
public class Visitor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID ID;

    @Column(nullable = false)
    private String fio;

    @Column
    private String subscription;

    @Column
    private double weight;

    @Column
    private double height;
}
