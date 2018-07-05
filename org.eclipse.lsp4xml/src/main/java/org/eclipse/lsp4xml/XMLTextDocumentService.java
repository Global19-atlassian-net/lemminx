/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lsp4xml;

import static org.eclipse.lsp4j.jsonrpc.CompletableFutures.computeAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4xml.commons.LanguageModelCache;
import org.eclipse.lsp4xml.commons.TextDocuments;
import org.eclipse.lsp4xml.internal.parser.XMLParser;
import org.eclipse.lsp4xml.model.XMLDocument;
import org.eclipse.lsp4xml.services.XMLLanguageService;

/**
 * XML text document service.
 *
 */
public class XMLTextDocumentService implements TextDocumentService {

	private final XMLLanguageServer xmlLanguageServer;
	private final TextDocuments documents;
	private final XMLLanguageService languageService;
	private final LanguageModelCache<XMLDocument> xmlDocuments;
	private final FormattingOptions sharedFormattingOptions;
	private CompletableFuture<Object> validationRequest;

	public XMLTextDocumentService(XMLLanguageServer fmLanguageServer) {
		this.xmlLanguageServer = fmLanguageServer;
		this.languageService = new XMLLanguageService();
		this.documents = new TextDocuments();
		XMLParser parser = XMLParser.getInstance();
		this.xmlDocuments = new LanguageModelCache<XMLDocument>(10, 60,
				document -> parser.parse(document.getText(), document.getUri()));
		this.sharedFormattingOptions = new FormattingOptions(4, false);

	}

	private XMLDocument getXMLDocument(TextDocumentItem document) {
		return xmlDocuments.get(document);
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			CompletionList list = languageService.doComplete(xmlDocument, params.getPosition(),
					sharedFormattingOptions);
			return Either.forRight(list);
		});
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			return languageService.doHover(xmlDocument, params.getPosition());
		});
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			return languageService.findDocumentHighlights(xmlDocument, params.getPosition());
		});
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			return languageService.findDocumentSymbols(xmlDocument);
		});
	}

	@Override
	public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return null;
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			return languageService.format(xmlDocument, null, params.getOptions());
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return computeAsync((monitor) -> {
			TextDocumentItem document = documents.get(params.getTextDocument().getUri());
			XMLDocument xmlDocument = getXMLDocument(document);
			return languageService.format(xmlDocument, params.getRange(), params.getOptions());
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return null;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		documents.onDidOpenTextDocument(params);
		triggerValidation(params.getTextDocument());
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		documents.onDidChangeTextDocument(params);
		TextDocumentItem document = documents.get(params.getTextDocument().getUri());
		if (document != null) {
			triggerValidation(document);
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		documents.onDidCloseTextDocument(params);
		xmlDocuments.onDocumentRemoved(params.getTextDocument().getUri());
		TextDocumentIdentifier document = params.getTextDocument();
		String uri = document.getUri();
		xmlLanguageServer.getLanguageClient()
				.publishDiagnostics(new PublishDiagnosticsParams(uri, new ArrayList<Diagnostic>()));

	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {

	}

	private void triggerValidation(TextDocumentItem document) {
		if (validationRequest != null) {
			validationRequest.cancel(true);
		}
		validationRequest = computeAsync((monitor) -> {
			monitor.checkCanceled();
			List<Diagnostic> diagnostics = languageService.doDiagnostics(document, null, monitor);
			monitor.checkCanceled();
			xmlLanguageServer.getLanguageClient().publishDiagnostics(new PublishDiagnosticsParams(document.getUri(), diagnostics));
			return null;
		});
	}

}
