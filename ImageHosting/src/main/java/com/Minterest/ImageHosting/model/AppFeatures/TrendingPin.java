package com.Minterest.ImageHosting.model.AppFeatures;

import com.Minterest.ImageHosting.model.Pin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pairs a Pin entity with its calculated trending score from Redis.
 * Used in the trending feed API response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingPin {
    private Pin pin;
    private double trendingScore;
    private int rank;
}
