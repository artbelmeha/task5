package com.task05;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
  private String id;
  private Long principalId;
  private String createdAt;
  private Map<String, String> body;
}
