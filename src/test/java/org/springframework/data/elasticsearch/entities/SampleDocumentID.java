package org.springframework.data.elasticsearch.entities;

import org.springframework.data.elasticsearch.core.DocumentID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleDocumentID implements DocumentID{
  
  private String id1;
  private String id2;
  
  @Override
  public String getDocumentId() {
    return id1+"."+id2;
  }

}
