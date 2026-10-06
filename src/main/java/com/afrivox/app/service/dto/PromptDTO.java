package com.afrivox.app.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Prompt} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PromptDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 500)
    private String content;

    private CampaignDTO campaign;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CampaignDTO getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignDTO campaign) {
        this.campaign = campaign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PromptDTO)) {
            return false;
        }

        PromptDTO promptDTO = (PromptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, promptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PromptDTO{" +
            "id=" + getId() +
            ", content='" + getContent() + "'" +
            ", campaign=" + getCampaign() +
            "}";
    }
}
