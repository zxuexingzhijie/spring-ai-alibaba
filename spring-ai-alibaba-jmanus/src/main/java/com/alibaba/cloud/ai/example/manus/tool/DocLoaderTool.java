/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class DocLoaderTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(DocLoaderTool.class);

	private static String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "file_type": {
			            "type": "string",
			            "description": "(required) File type, only support pdf file."
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required) Get the absolute path of the file from the user request."
			        }
			    },
			    "required": ["file_type","file_path"]
			}
			""";

	private static final String name = "doc_loader";

	private static final String description = """
			Get the content information of a local file at a specified path.
			Use this tool when you want to get some related information asked by the user.
			This tool accepts the file path and gets the related information content.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new DocLoaderTool()) // 修改为正确的工具类
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public DocLoaderTool() {
	}

	private String lastFilePath = "";

	private String lastOperationResult = "";

	private String lastFileType = "";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public ToolExecuteResult run(String toolInput) {
		log.info("DocLoaderTool toolInput:{}", toolInput);
		try {
			Map<String, Object> toolInputMap = objectMapper.readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String fileType = (String) toolInputMap.get("file_type");
			String filePath = (String) toolInputMap.get("file_path");
			this.lastFilePath = filePath;
			this.lastFileType = fileType;

			if (!"pdf".equalsIgnoreCase(fileType)) {
				return new ToolExecuteResult("Unsupported file type: " + fileType);
			}

			try (PDDocument document = PDDocument.load(new File(filePath))) {
				PDFTextStripper pdfStripper = new PDFTextStripper();
				String documentContentStr = pdfStripper.getText(document);

				if (StringUtils.isEmpty(documentContentStr)) {
					this.lastOperationResult = "No content found";
					return new ToolExecuteResult("No Related information");
				}
				else {
					this.lastOperationResult = "Success";
					return new ToolExecuteResult("Related information: " + documentContentStr);
				}
			}
		}
		catch (Throwable e) {
			this.lastOperationResult = "Error: " + e.getMessage();
			return new ToolExecuteResult("Error get Related information: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<?> getInputType() {
		return String.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public ToolExecuteResult apply(String t, ToolContext u) {
		return run(t);
	}

	@Override
	public void setPlanId(String planId) {
		// Implementation for setting planId if required in the future.
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				            Current File Operation State:
				            - Working Directory:
				%s

				            - Last File Operation:
				%s

				            - Last Operation Result:
				%s

				            """, new File("").getAbsolutePath(),
				lastFilePath.isEmpty() ? "No file loaded yet"
						: String.format("Load %s file from: %s", lastFileType, lastFilePath),
				lastOperationResult.isEmpty() ? "No operation performed yet" : lastOperationResult);
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

}
