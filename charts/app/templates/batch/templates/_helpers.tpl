{{/*
Expand the name of the chart.
*/}}
{{- define "batch.name" -}}
{{- printf "batch" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "batch.fullname" -}}
{{- $componentName := include "batch.name" .  }}
{{- if .Values.batch.fullnameOverride }}
{{- .Values.batch.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $componentName | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "batch.labels" -}}
{{ include "batch.selectorLabels" . }}
{{- if .Values.global.tag }}
app.kubernetes.io/image-version: {{ .Values.global.tag | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/short-name: {{ include "batch.name" . }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "batch.selectorLabels" -}}
app.kubernetes.io/name: {{ include "batch.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


