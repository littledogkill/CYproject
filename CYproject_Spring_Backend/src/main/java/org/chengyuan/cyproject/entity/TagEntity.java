package org.chengyuan.cyproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "taglist")
public class TagEntity {
    @Id
    @Column(name = "id", length = 255)
    String id;

    @Column(name= "tags", length = 255)
    String tags;
}
