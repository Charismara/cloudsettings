package com.github.manasmods.cloudsettings.cloudservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Setting {
    @Getter
    private String key, value;
}
