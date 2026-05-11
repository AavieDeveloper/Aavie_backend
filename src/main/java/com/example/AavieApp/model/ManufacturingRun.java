package com.example.AavieApp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "manufacturing_runs")
public class ManufacturingRun {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;
	private String assessmentType;
	
	@Column(columnDefinition = "LONGTEXT")
	private String formulaJson;

	@Column(columnDefinition = "LONGTEXT")
	private String doshaPct;
	
	private String severity;
	private String prakriti;
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private boolean revealed = false; // false = not shown to free users

	public ManufacturingRun() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ManufacturingRun(Long id, Long userId, String assessmentType, String formulaJson, String doshaPct,
			String severity, String prakriti, LocalDateTime createdAt, boolean revealed) {
		super();
		this.id = id;
		this.userId = userId;
		this.assessmentType = assessmentType;
		this.formulaJson = formulaJson;
		this.doshaPct = doshaPct;
		this.severity = severity;
		this.prakriti = prakriti;
		this.createdAt = createdAt;
		this.revealed = revealed;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getAssessmentType() {
		return assessmentType;
	}

	public void setAssessmentType(String assessmentType) {
		this.assessmentType = assessmentType;
	}

	public String getFormulaJson() {
		return formulaJson;
	}

	public void setFormulaJson(String formulaJson) {
		this.formulaJson = formulaJson;
	}

	public String getDoshaPct() {
		return doshaPct;
	}

	public void setDoshaPct(String doshaPct) {
		this.doshaPct = doshaPct;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getPrakriti() {
		return prakriti;
	}

	public void setPrakriti(String prakriti) {
		this.prakriti = prakriti;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isRevealed() {
		return revealed;
	}

	public void setRevealed(boolean revealed) {
		this.revealed = revealed;
	}

	@Override
	public String toString() {
		return "ManufacturingRun [id=" + id + ", userId=" + userId + ", assessmentType=" + assessmentType
				+ ", formulaJson=" + formulaJson + ", doshaPct=" + doshaPct + ", severity=" + severity + ", prakriti="
				+ prakriti + ", createdAt=" + createdAt + ", revealed=" + revealed + "]";
	}

}