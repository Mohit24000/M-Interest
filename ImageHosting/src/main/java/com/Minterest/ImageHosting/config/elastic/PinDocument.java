package com.Minterest.ImageHosting.config.elastic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "pins")
@Setting(settingPath = "elasticsearch/settings.json")
public class PinDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String pinId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard")
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String pinUrl;

    @Field(type = FieldType.Keyword)
    private String downloadUrl;

    @Field(type = FieldType.Date)
    private String uploadedAt;

    @Field(type = FieldType.Long)
    private long saves;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard")
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Integer)
    private int likeCount;
}
