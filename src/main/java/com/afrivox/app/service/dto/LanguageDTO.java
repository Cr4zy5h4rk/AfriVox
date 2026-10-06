package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.LanguageStatus;
import com.afrivox.app.domain.enumeration.ScriptKind;
import com.afrivox.app.domain.enumeration.TextDirection;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Language} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LanguageDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 10)
    private String code;

    @NotNull
    @Size(max = 80)
    private String name;

    @Size(max = 80)
    private String dialect;

    @NotNull
    private ScriptKind script;

    @NotNull
    private TextDirection direction;

    @NotNull
    private LanguageStatus status;

    @NotNull
    @Min(value = 1)
    private Integer minValidators;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public ScriptKind getScript() {
        return script;
    }

    public void setScript(ScriptKind script) {
        this.script = script;
    }

    public TextDirection getDirection() {
        return direction;
    }

    public void setDirection(TextDirection direction) {
        this.direction = direction;
    }

    public LanguageStatus getStatus() {
        return status;
    }

    public void setStatus(LanguageStatus status) {
        this.status = status;
    }

    public Integer getMinValidators() {
        return minValidators;
    }

    public void setMinValidators(Integer minValidators) {
        this.minValidators = minValidators;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LanguageDTO)) {
            return false;
        }

        LanguageDTO languageDTO = (LanguageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, languageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LanguageDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", dialect='" + getDialect() + "'" +
            ", script='" + getScript() + "'" +
            ", direction='" + getDirection() + "'" +
            ", status='" + getStatus() + "'" +
            ", minValidators=" + getMinValidators() +
            "}";
    }
}
