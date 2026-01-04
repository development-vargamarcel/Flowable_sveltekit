<script lang="ts">
	interface Suggestion {
		label: string;
		value: string;
		type: 'field' | 'grid' | 'column' | 'variable' | 'function';
		description?: string;
	}

	interface Props {
		value: string;
		language?: 'javascript' | 'sql' | 'expression';
		suggestions?: Suggestion[];
		placeholder?: string;
		rows?: number;
		onchange: (value: string) => void;
		readonly?: boolean;
	}

	const {
		value = '',
		language: _language = 'javascript',
		suggestions = [],
		placeholder = '',
		rows = 4,
		onchange,
		readonly = false
	}: Props = $props();

	// Autocomplete state
	let showAutocomplete = $state(false);
	let autocompletePosition = $state({ top: 0, left: 0 });
	let filteredSuggestions = $state<Suggestion[]>([]);
	let selectedIndex = $state(0);
	let searchTerm = $state('');
	let textareaRef: HTMLTextAreaElement;

	// Syntax highlighting tokens for display (reserved for future use)
	const _syntaxPatterns = {
		keyword: /\b(return|if|else|for|while|function|const|let|var|true|false|null|undefined)\b/g,
		string: /(["'`])(?:(?!\1)[^\\]|\\.)*\1/g,
		number: /\b\d+(?:\.\d+)?\b/g,
		variable: /\$\{[^}]+\}/g,
		comment: /\/\/.*$/gm
	};

	function handleInput(e: Event) {
		const target = e.target as HTMLTextAreaElement;
		const newValue = target.value;
		onchange(newValue);
		
		// Check for autocomplete trigger
		checkAutocomplete(target);
	}

	function checkAutocomplete(textarea: HTMLTextAreaElement) {
		const cursorPos = textarea.selectionStart;
		const textBeforeCursor = textarea.value.substring(0, cursorPos);
		
		// Find the current word being typed
		const wordMatch = textBeforeCursor.match(/[\w.]+$/);
		if (wordMatch && wordMatch[0].length >= 1) {
			searchTerm = wordMatch[0].toLowerCase();
			
			// Filter suggestions
			filteredSuggestions = suggestions.filter(s => 
				s.label.toLowerCase().includes(searchTerm) ||
				s.value.toLowerCase().includes(searchTerm)
			).slice(0, 8);

			if (filteredSuggestions.length > 0) {
				// Position autocomplete near cursor
				const rect = textarea.getBoundingClientRect();
				const lineHeight = parseInt(getComputedStyle(textarea).lineHeight) || 20;
				const lines = textBeforeCursor.split('\n');
				const currentLine = lines.length - 1;
				
				autocompletePosition = {
					top: currentLine * lineHeight + lineHeight + 4,
					left: Math.min((lines[currentLine]?.length || 0) * 8, rect.width - 200)
				};
				showAutocomplete = true;
				selectedIndex = 0;
			} else {
				showAutocomplete = false;
			}
		} else {
			showAutocomplete = false;
		}
	}

	function handleKeyDown(e: KeyboardEvent) {
		if (!showAutocomplete) return;

		switch (e.key) {
			case 'ArrowDown':
				e.preventDefault();
				selectedIndex = Math.min(selectedIndex + 1, filteredSuggestions.length - 1);
				break;
			case 'ArrowUp':
				e.preventDefault();
				selectedIndex = Math.max(selectedIndex - 1, 0);
				break;
			case 'Enter':
			case 'Tab':
				if (filteredSuggestions.length > 0) {
					e.preventDefault();
					insertSuggestion(filteredSuggestions[selectedIndex]);
				}
				break;
			case 'Escape':
				showAutocomplete = false;
				break;
		}
	}

	function insertSuggestion(suggestion: Suggestion) {
		const cursorPos = textareaRef.selectionStart;
		const textBeforeCursor = value.substring(0, cursorPos);
		const textAfterCursor = value.substring(cursorPos);
		
		// Remove the partial word that was typed
		const wordMatch = textBeforeCursor.match(/[\w.]+$/);
		const startPos = cursorPos - (wordMatch ? wordMatch[0].length : 0);
		
		const newValue = value.substring(0, startPos) + suggestion.value + textAfterCursor;
		onchange(newValue);
		
		showAutocomplete = false;
		
		// Set cursor position after inserted text
		setTimeout(() => {
			const newCursorPos = startPos + suggestion.value.length;
			textareaRef.setSelectionRange(newCursorPos, newCursorPos);
			textareaRef.focus();
		}, 0);
	}

	function handleBlur() {
		// Delay hiding to allow click on suggestion
		setTimeout(() => {
			showAutocomplete = false;
		}, 150);
	}

	function getTypeIcon(type: Suggestion['type']): string {
		switch (type) {
			case 'field': return '📝';
			case 'grid': return '📊';
			case 'column': return '📋';
			case 'variable': return '📦';
			case 'function': return 'ƒ';
			default: return '•';
		}
	}

	function getTypeColor(type: Suggestion['type']): string {
		switch (type) {
			case 'field': return 'text-blue-600';
			case 'grid': return 'text-green-600';
			case 'column': return 'text-purple-600';
			case 'variable': return 'text-orange-600';
			case 'function': return 'text-pink-600';
			default: return 'text-gray-600';
		}
	}
</script>

<div class="relative">
	<textarea
		bind:this={textareaRef}
		{value}
		oninput={handleInput}
		onkeydown={handleKeyDown}
		onblur={handleBlur}
		{placeholder}
		{rows}
		disabled={readonly}
		class="code-editor w-full font-mono text-sm rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 {readonly ? 'bg-gray-50 cursor-not-allowed' : ''}"
		spellcheck="false"
		autocomplete="off"
	></textarea>

	{#if showAutocomplete && filteredSuggestions.length > 0}
		<div 
			class="absolute z-50 bg-white border border-gray-200 rounded-md shadow-lg max-h-48 overflow-y-auto min-w-48"
			style="top: {autocompletePosition.top}px; left: {autocompletePosition.left}px;"
		>
			{#each filteredSuggestions as suggestion, index}
				<button
					type="button"
					class="w-full px-3 py-1.5 text-left text-sm flex items-center gap-2 hover:bg-blue-50 {index === selectedIndex ? 'bg-blue-100' : ''}"
					onclick={() => insertSuggestion(suggestion)}
				>
					<span class="flex-shrink-0">{getTypeIcon(suggestion.type)}</span>
					<span class="flex-1 font-medium {getTypeColor(suggestion.type)}">{suggestion.label}</span>
					{#if suggestion.description}
						<span class="text-xs text-gray-400 truncate max-w-24">{suggestion.description}</span>
					{/if}
				</button>
			{/each}
		</div>
	{/if}
</div>

<style>
	.code-editor {
		tab-size: 2;
		white-space: pre-wrap;
		word-wrap: break-word;
	}

	.code-editor:focus {
		outline: none;
	}
</style>
