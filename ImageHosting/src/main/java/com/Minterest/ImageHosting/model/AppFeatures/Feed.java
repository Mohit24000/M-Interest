package com.Minterest.ImageHosting.model.AppFeatures;

import com.Minterest.ImageHosting.model.Pin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a paginated feed of pins (e.g. home feed or trending feed).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feed {
    private List<Pin> pins;
    private int page;
    private int size;
    private long totalPins;
}
