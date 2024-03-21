/*
 * Generated on 3/16/21, 2:05 PM
 */
package org.hkijena.jipipe.extensions.r.ui;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * 
 */
%%

%public
%class RTokenMaker
%extends AbstractJFlexCTokenMaker
%unicode
/* Case sensitive */
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public RTokenMaker() {
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
	}


	/**
	 * {@inheritDoc}
	 */
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return new String[] { "#", null };
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			/* No multi-line comments */
			/* No documentation comments */
			default:
				state = Token.NULL;
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new TokenImpl();
		}

	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

Letter							= [A-Za-z]
LetterOrUnderscore				= ({Letter}|"_")
NonzeroDigit						= [1-9]
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
AnyCharacterButApostropheOrBackSlash	= ([^\\'])
AnyCharacterButDoubleQuoteOrBackSlash	= ([^\\\"\n])
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
Escape							= ("\\"(([btnfr\"'\\])|([0123]{OctalDigit}?{OctalDigit}?)|({OctalDigit}{OctalDigit}?)|{EscapedSourceCharacter}))
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#"|"\\")
IdentifierStart					= ({LetterOrUnderscore}|"$")
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))

LineTerminator				= (\n)
WhiteSpace				= ([ \t\f]+)

/* No char literals */
/* No string macros */

/* No multi-line comments */
/* No documentation comments */
LineCommentBegin			= "#"

IntegerLiteral			= ({Digit}+)
HexLiteral			= (0x{HexDigit}+)
FloatLiteral			= (({Digit}+)("."{Digit}+)?(e[+-]?{Digit}+)? | ({Digit}+)?("."{Digit}+)(e[+-]?{Digit}+)?)
ErrorNumberFormat			= (({IntegerLiteral}|{HexLiteral}|{FloatLiteral}){NonSeparator}+)
BooleanLiteral				= ("true"|"false")

Separator					= ([\(\)\{\}\[\]])
Separator2				= ([\;,.])

Identifier				= ({IdentifierStart}{IdentifierPart}*)

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


%state STRING
/* No char state */
/* No MLC state */
/* No documentation comment state */
%state EOL_COMMENT

%%

<YYINITIAL> {

	/* Keywords */
	"FALSE" |
"Inf" |
"NA" |
"NA_character_" |
"NA_complex_" |
"NA_integer_" |
"NA_real_" |
"NULL" |
"NaN" |
"TRUE" |
"break" |
"else" |
"for" |
"function" |
"if" |
"in" |
"next" |
"repeat" |
"while"		{ addToken(Token.RESERVED_WORD); }

	/* Keywords 2 (just an optional set of keywords colored differently) */
	/* No keywords 2 */

	/* Data types */
	/* No data types */

	/* Functions */
	"-- M --" |
"Arg" |
"Arithmetic" |
"AsIs" |
"Autoloads" |
"Bessel" |
"Comparison" |
"Complex" |
"Conj" |
"Constants" |
"Control" |
"Cstack_info" |
"DLLInfo" |
"DLLInfoList" |
"DLLpath" |
"Date" |
"DateTimeClasses" |
"Dates" |
"Defunct" |
"Deprecated" |
"Encoding" |
"Extract" |
"F" |
"FALSE" |
"Filter" |
"Find" |
"Foreign" |
"GSC" |
"HOME" |
"I" |
"ISOdate" |
"ISOdatetime" |
"Im" |
"Inf" |
"InternalGenerics" |
"InternalMethods" |
"LANGUAGE" |
"LC_ALL" |
"LC_COLLATE" |
"LC_CTYPE" |
"LC_MONETARY" |
"LC_NUMERIC" |
"LC_TIME" |
"LETTERS" |
"La.svd" |
"La_library" |
"La_version" |
"Logic" |
"Long vectors" |
"MAKEINDEX" |
"MC_CORES" |
"Map" |
"Math" |
"Math.Date" |
"Math.POSIXlt" |
"Math.POSIXt" |
"Math.difftime" |
"Math.factor" |
"Memory" |
"Memory-limits" |
"Mod" |
"NCOL" |
"NROW" |
"NativeSymbol" |
"NativeSymbolInfo" |
"Negate" |
"NextMethod" |
"NotYetImplemented" |
"NotYetUsed" |
"NumericConstants" |
"OlsonNames" |
"Ops" |
"Ops.Date" |
"Ops.POSIXt" |
"Ops.difftime" |
"Ops.factor" |
"Ops.numeric_version" |
"Ops.ordered" |
"POSIXct" |
"POSIXlt" |
"POSIXt" |
"Paren" |
"Position" |
"Quotes" |
"R.Version" |
"R.home" |
"R.version.string" |
"RD2PDF_INPUTENC" |
"RNG" |
"RNGkind" |
"RNGversion" |
"Random" |
"Random.user" |
"Rd2pdf" |
"Rdconv" |
"Re" |
"Recall" |
"Reduce" |
"RegisteredNativeSymbol" |
"Renviron" |
"Reserved" |
"S3Methods" |
"S3groupGeneric" |
"S4" |
"Special" |
"Startup" |
"Subscript" |
"Summary" |
"Summary.Date" |
"Summary.POSIXct" |
"Summary.POSIXlt" |
"Summary.difftime" |
"Summary.factor" |
"Summary.numeric_version" |
"Summary.ordered" |
"Syntax" |
"Sys.Date" |
"Sys.chmod" |
"Sys.getenv" |
"Sys.getlocale" |
"Sys.getpid" |
"Sys.glob" |
"Sys.info" |
"Sys.junction" |
"Sys.localeconv" |
"Sys.readlink" |
"Sys.setFileTime" |
"Sys.setenv" |
"Sys.setlocale" |
"Sys.sleep" |
"Sys.time" |
"Sys.timezone" |
"Sys.umask" |
"Sys.unsetenv" |
"Sys.which" |
"T" |
"TMPDIR" |
"TRUE" |
"TZ" |
"TZDIR" |
"Trig" |
"UTF-8 file path" |
"Unicode" |
"UseMethod" |
"Vectorize" |
"abbreviate" |
"abs" |
"acos" |
"acosh" |
"activeBindingFunction" |
"addNA" |
"addTaskCallback" |
"agrep" |
"agrepl" |
"alist" |
"all" |
"all.equal" |
"all.names" |
"all.vars" |
"allowInterrupts" |
"any" |
"anyDuplicated" |
"anyMissing" |
"anyNA" |
"anyNA.POSIXlt" |
"anyNA.numeric_version" |
"aperm" |
"append" |
"apply" |
"arccos" |
"arcsin" |
"arctan" |
"args" |
"array" |
"arrayInd" |
"as.Date" |
"as.POSIXct" |
"as.POSIXlt" |
"as.array" |
"as.call" |
"as.character" |
"as.character.Date" |
"as.character.POSIXt" |
"as.character.condition" |
"as.character.hexmode" |
"as.character.numeric_version" |
"as.character.octmode" |
"as.character.srcref" |
"as.complex" |
"as.data.frame" |
"as.data.frame.AsIs" |
"as.data.frame.Date" |
"as.data.frame.POSIXct" |
"as.data.frame.numeric_version" |
"as.data.frame.table" |
"as.difftime" |
"as.double" |
"as.double.POSIXlt" |
"as.double.difftime" |
"as.environment" |
"as.expression" |
"as.factor" |
"as.function" |
"as.hexmode" |
"as.integer" |
"as.list" |
"as.list.Date" |
"as.list.POSIXct" |
"as.list.difftime" |
"as.list.numeric_version" |
"as.logical" |
"as.matrix" |
"as.matrix.POSIXlt" |
"as.matrix.noquote" |
"as.name" |
"as.null" |
"as.numeric" |
"as.numeric_version" |
"as.octmode" |
"as.ordered" |
"as.package_version" |
"as.pairlist" |
"as.qr" |
"as.raw" |
"as.single" |
"as.symbol" |
"as.table" |
"as.vector" |
"asS3" |
"asS4" |
"asin" |
"asinh" |
"asplit" |
"assign" |
"assignOps" |
"atan" |
"atan2" |
"atanh" |
"atomic" |
"attach" |
"attachNamespace" |
"attr" |
"attr.all.equal" |
"attributes" |
"autoload" |
"autoloader" |
"backquote" |
"backsolve" |
"backtick" |
"base" |
"baseenv" |
"basename" |
"bessel" |
"besselI" |
"besselJ" |
"besselK" |
"besselY" |
"beta" |
"bindenv" |
"bindingIsActive" |
"bindingIsLocked" |
"bindtextdomain" |
"bitwAnd" |
"bitwNot" |
"bitwOr" |
"bitwShiftL" |
"bitwShiftR" |
"bitwXor" |
"body" |
"bquote" |
"break" |
"browser" |
"browserCondition" |
"browserSetDebug" |
"browserText" |
"builtins" |
"by" |
"bzfile" |
"c" |
"c.Date" |
"c.POSIXct" |
"c.difftime" |
"c.noquote" |
"c.numeric_version" |
"c.warnings" |
"call" |
"callCC" |
"capabilities" |
"casefold" |
"cat" |
"cbind" |
"cbind.data.frame" |
"ceiling" |
"char.expand" |
"charToRaw" |
"character" |
"charmatch" |
"chartr" |
"check_tzones" |
"chkDots" |
"chol" |
"chol2inv" |
"choose" |
"class" |
"clearPushBack" |
"clipboard" |
"close" |
"close.srcfile" |
"close.srcfilealias" |
"closeAllConnections" |
"closure" |
"code point" |
"col" |
"colMeans" |
"colSums" |
"collation" |
"colnames" |
"colon" |
"commandArgs" |
"comment" |
"complex" |
"computeRestarts" |
"condition" |
"conditionCall" |
"conditionCall.condition" |
"conditionMessage" |
"conditionMessage.condition" |
"conditions" |
"conflictRules" |
"conflicts" |
"connection" |
"connections" |
"contributors" |
"copyright" |
"copyrights" |
"cos" |
"cosh" |
"cospi" |
"crossprod" |
"cummax" |
"cummin" |
"cumprod" |
"cumsum" |
"curlGetHeaders" |
"cut" |
"cut.POSIXt" |
"dQuote" |
"data.class" |
"data.frame" |
"data.matrix" |
"date" |
"date-time" |
"debug" |
"debuggingState" |
"debugonce" |
"default.stringsAsFactors" |
"defunct" |
"delayedAssign" |
"deparse" |
"deparse1" |
"deprecated" |
"det" |
"detach" |
"determinant" |
"dget" |
"diag" |
"diff" |
"diff.difftime" |
"difftime" |
"digamma" |
"dim" |
"dimnames" |
"dir" |
"dir.create" |
"dirname" |
"do.call" |
"dontCheck" |
"dots" |
"double" |
"dput" |
"drop" |
"droplevels" |
"dump" |
"duplicated" |
"duplicated.POSIXlt" |
"duplicated.numeric_version" |
"duplicated.warnings" |
"dyn.load" |
"dyn.unload" |
"dynGet" |
"eapply" |
"eigen" |
"else" |
"emptyenv" |
"enc2native" |
"enc2utf8" |
"enclosure" |
"encodeString" |
"endsWith" |
"enquote" |
"env.profile" |
"environment" |
"environment variables" |
"environmentIsLocked" |
"environmentName" |
"errorCondition" |
"eval" |
"eval.parent" |
"evalq" |
"exists" |
"exp" |
"expand.grid" |
"expm1" |
"expression" |
"extSoftVersion" |
"factor" |
"factorial" |
"fifo" |
"file" |
"file path encoding" |
"file.access" |
"file.append" |
"file.choose" |
"file.copy" |
"file.create" |
"file.exists" |
"file.info" |
"file.link" |
"file.mode" |
"file.mtime" |
"file.path" |
"file.remove" |
"file.rename" |
"file.show" |
"file.size" |
"file.symlink" |
"files" |
"finalizer" |
"find.package" |
"findInterval" |
"findRestart" |
"finite" |
"floor" |
"flush" |
"for" |
"force" |
"forceAndCall" |
"formals" |
"format" |
"format.Date" |
"format.POSIXct" |
"format.difftime" |
"format.hexmode" |
"format.info" |
"format.libraryIQR" |
"format.numeric_version" |
"format.octmode" |
"format.packageInfo" |
"format.pval" |
"format.summaryDefault" |
"formatC" |
"formatDL" |
"forwardsolve" |
"function" |
"fuzzy matching" |
"gamma" |
"gc" |
"gc.time" |
"gcinfo" |
"gctorture" |
"gctorture2" |
"get" |
"get0" |
"getAllConnections" |
"getConnection" |
"getDLLRegisteredRoutines" |
"getElement" |
"getHook" |
"getLoadedDLLs" |
"getNativeSymbolInfo" |
"getOption" |
"getRversion" |
"getSrcLines" |
"getTaskCallbackNames" |
"geterrmessage" |
"gettext" |
"gettextf" |
"getwd" |
"gl" |
"globalCallingHandlers" |
"globalenv" |
"gregexpr" |
"grep" |
"grepRaw" |
"grepl" |
"group generic" |
"groupGeneric" |
"grouping" |
"gsub" |
"gzcon" |
"gzfile" |
"hexmode" |
"iconv" |
"iconvlist" |
"icuGetCollate" |
"icuSetCollate" |
"identical" |
"identity" |
"if" |
"ifelse" |
"in" |
"infinite" |
"infoRDS" |
"inherits" |
"intToBits" |
"intToUtf8" |
"integer" |
"interaction" |
"interactive" |
"internal generic" |
"intersect" |
"intersection" |
"inverse.rle" |
"invisible" |
"invokeRestart" |
"invokeRestartInteractively" |
"is.R" |
"is.array" |
"is.atomic" |
"is.call" |
"is.character" |
"is.complex" |
"is.data.frame" |
"is.double" |
"is.element" |
"is.environment" |
"is.expression" |
"is.factor" |
"is.finite" |
"is.function" |
"is.infinite" |
"is.integer" |
"is.language" |
"is.list" |
"is.loaded" |
"is.logical" |
"is.matrix" |
"is.na" |
"is.na.POSIXlt" |
"is.na.numeric_version" |
"is.name" |
"is.nan" |
"is.null" |
"is.numeric" |
"is.numeric.difftime" |
"is.numeric_version" |
"is.object" |
"is.ordered" |
"is.package_version" |
"is.pairlist" |
"is.primitive" |
"is.qr" |
"is.raw" |
"is.recursive" |
"is.single" |
"is.symbol" |
"is.table" |
"is.unsorted" |
"is.vector" |
"isFALSE" |
"isIncomplete" |
"isNamespaceLoaded" |
"isOpen" |
"isRestart" |
"isS4" |
"isSeekable" |
"isSymmetric" |
"isTRUE" |
"isatty" |
"isdebugged" |
"jitter" |
"julian" |
"kappa" |
"kronecker" |
"l10n_info" |
"labels" |
"language" |
"language object" |
"language objects" |
"lapply" |
"last.warning" |
"lbeta" |
"lchoose" |
"length" |
"length.POSIXlt" |
"lengths" |
"letters" |
"levels" |
"lfactorial" |
"lgamma" |
"libcurlVersion" |
"library" |
"library.dynam" |
"library.dynam.unload" |
"licence" |
"license" |
"list" |
"list.dirs" |
"list.files" |
"list2DF" |
"list2env" |
"load" |
"loadNamespace" |
"loadedNamespaces" |
"local" |
"localeconv" |
"locales" |
"lockBinding" |
"lockEnvironment" |
"log" |
"log10" |
"log1p" |
"log2" |
"logb" |
"logical" |
"long vector" |
"long vectors" |
"lower.tri" |
"ls" |
"make.names" |
"make.unique" |
"makeActiveBinding" |
"mapply" |
"margin.table" |
"marginSums" |
"mat.or.vec" |
"match" |
"match.arg" |
"match.call" |
"match.fun" |
"matmult" |
"matrix" |
"max" |
"max.col" |
"mean" |
"mean.Date" |
"mean.POSIXct" |
"mean.difftime" |
"mem.maxVSize" |
"memCompress" |
"memDecompress" |
"memory.profile" |
"merge" |
"issue" |
"mget" |
"min" |
"missing" |
"mode" |
"month.abb" |
"months" |
"name" |
"names" |
"names.POSIXlt" |
"nargs" |
"nchar" |
"ncol" |
"new.env" |
"next" |
"ngettext" |
"nlevels" |
"noquote" |
"norm" |
"normalizePath" |
"nrow" |
"nullfile" |
"numeric" |
"numeric_version" |
"nzchar" |
"objects" |
"octmode" |
"oldClass" |
"on.exit" |
"open" |
"open.srcfile" |
"open.srcfilealias" |
"option" |
"options" |
"order" |
"ordered" |
"outer" |
"packBits" |
"packageEvent" |
"packageNotFoundError" |
"packageStartupMessage" |
"package_version" |
"pairlist" |
"parent.env" |
"parent.frame" |
"parse" |
"paste" |
"paste0" |
"path.expand" |
"path.package" |
"pcre_config" |
"pi" |
"pipe" |
"plot" |
"pmatch" |
"pmax" |
"pmax.int" |
"pmin" |
"pmin.int" |
"polyroot" |
"pos.to.env" |
"pretty" |
"prettyNum" |
"primitive" |
"print" |
"print.AsIs" |
"print.DLLInfo" |
"print.Date" |
"print.Dlist" |
"print.NativeRoutineList" |
"print.POSIXct" |
"print.by" |
"print.condition" |
"print.connection" |
"print.data.frame" |
"print.default" |
"print.difftime" |
"print.eigen" |
"print.hexmode" |
"print.libraryIQR" |
"print.noquote" |
"print.numeric_version" |
"print.octmode" |
"print.packageInfo" |
"print.proc_time" |
"print.rle" |
"print.simple.list" |
"print.srcfile" |
"print.srcref" |
"print.summary.table" |
"print.summary.warnings" |
"print.summaryDefault" |
"print.warnings" |
"prmatrix" |
"proc.time" |
"prod" |
"promise" |
"promises" |
"prop.table" |
"proportions" |
"provideDimnames" |
"psigamma" |
"pushBack" |
"pushBackLength" |
"q" |
"qr" |
"qr.Q" |
"qr.R" |
"qr.X" |
"qr.coef" |
"qr.default" |
"qr.fitted" |
"qr.qty" |
"qr.qy" |
"qr.resid" |
"qr.solve" |
"quarters" |
"quit" |
"quote" |
"range" |
"rank" |
"rapply" |
"raw" |
"rawConnection" |
"rawConnectionValue" |
"rawShift" |
"rawToBits" |
"rawToChar" |
"rbind" |
"rbind.data.frame" |
"rcond" |
"read.dcf" |
"readBin" |
"readChar" |
"readLines" |
"readRDS" |
"readRenviron" |
"readline" |
"reg.finalizer" |
"regex" |
"regexec" |
"regexp" |
"regexpr" |
"regmatches" |
"regular expression" |
"remove" |
"removeTaskCallback" |
"rep" |
"rep.numeric_version" |
"rep_len" |
"repeat" |
"replace" |
"replicate" |
"require" |
"requireNamespace" |
"reserved" |
"restartDescription" |
"restartFormals" |
"retracemem" |
"return" |
"returnValue" |
"rev" |
"rle" |
"rm" |
"round" |
"round.Date" |
"round.POSIXt" |
"row" |
"row.names" |
"rowMeans" |
"rowSums" |
"rownames" |
"rowsum" |
"sQuote" |
"sample" |
"sapply" |
"save" |
"saveRDS" |
"scale" |
"scan" |
"search" |
"searchpaths" |
"seek" |
"seq" |
"seq.Date" |
"seq.POSIXt" |
"seq_along" |
"seq_len" |
"sequence" |
"serialize" |
"serverSocket" |
"set.seed" |
"setHook" |
"setSessionTimeLimit" |
"setTimeLimit" |
"setdiff" |
"setequal" |
"setwd" |
"shQuote" |
"shell" |
"shell.exec" |
"showConnections" |
"sign" |
"signalCondition" |
"signif" |
"simpleCondition" |
"simpleError" |
"simpleMessage" |
"simpleWarning" |
"simplify2array" |
"sin" |
"single" |
"sinh" |
"sink" |
"sinpi" |
"slice.index" |
"socketAccept" |
"socketConnection" |
"socketSelect" |
"socketTimeout" |
"solve" |
"solve.qr" |
"sort" |
"sort.list" |
"source" |
"split" |
"split.Date" |
"split.POSIXct" |
"sprintf" |
"sqrt" |
"srcfile" |
"srcfile-class" |
"srcfilealias" |
"srcfilealias-class" |
"srcfilecopy" |
"srcfilecopy-class" |
"srcref" |
"srcref-class" |
"standardGeneric" |
"startsWith" |
"stderr" |
"stdin" |
"stdout" |
"stop" |
"stopifnot" |
"storage.mode" |
"str.POSIXt" |
"str2expression" |
"str2lang" |
"strftime" |
"strptime" |
"strrep" |
"strsplit" |
"strtoi" |
"strtrim" |
"structure" |
"strwrap" |
"sub" |
"subset" |
"substitute" |
"substr" |
"substring" |
"sum" |
"summary" |
"summary.Date" |
"summary.POSIXct" |
"summary.connection" |
"summary.proc_time" |
"summary.srcfile" |
"summary.srcref" |
"summary.table" |
"summary.warnings" |
"suppressMessages" |
"suppressPackageStartupMessages" |
"suppressWarnings" |
"suspendInterrupts" |
"svd" |
"sweep" |
"switch" |
"sys.on.exit" |
"sys.parent" |
"sys.source" |
"sys.status" |
"system" |
"system.file" |
"system.time" |
"system2" |
"t" |
"table" |
"tabulate" |
"tan" |
"tanh" |
"tanpi" |
"tapply" |
"taskCallbackManager" |
"tcrossprod" |
"tempdir" |
"tempfile" |
"textConnection" |
"textConnectionValue" |
"tilde" |
"tilde expansion" |
"time interval" |
"time zone" |
"time zones" |
"timezone" |
"timezones" |
"toString" |
"tolower" |
"topenv" |
"toupper" |
"trace" |
"traceback" |
"tracemem" |
"tracingState" |
"transform" |
"trigamma" |
"trimws" |
"trunc" |
"trunc.Date" |
"trunc.POSIXt" |
"truncate" |
"try" |
"tryCatch" |
"tryInvokeRestart" |
"type" |
"typeof" |
"umask" |
"unclass" |
"undebug" |
"union" |
"unique" |
"unique.POSIXlt" |
"unique.numeric_version" |
"unique.warnings" |
"units" |
"units.difftime" |
"unlink" |
"unlist" |
"unloadNamespace" |
"unlockBinding" |
"unname" |
"unserialize" |
"unsplit" |
"untrace" |
"untracemem" |
"unz" |
"upper.tri" |
"url" |
"utf8ToInt" |
"validEnc" |
"validUTF8" |
"vapply" |
"vector" |
"version" |
"warning" |
"warningCondition" |
"warnings" |
"weekdays" |
"which" |
"which.max" |
"which.min" |
"while" |
"with" |
"withAutoprint" |
"withCallingHandlers" |
"withRestarts" |
"withVisible" |
"within" |
"write" |
"write.dcf" |
"writeBin" |
"writeChar" |
"writeLines" |
"xor" |
"xtfrm" |
"xtfrm.numeric_version" |
"xzfile" |
"zapsmall"		{ addToken(Token.FUNCTION); }

	{BooleanLiteral}			{ addToken(Token.LITERAL_BOOLEAN); }

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{Identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	/* No char literals */
	\"							{ start = zzMarkedPos-1; yybegin(STRING); }

	/* Comment literals. */
	/* No multi-line comments */
	/* No documentation comments */
	{LineCommentBegin}			{ start = zzMarkedPos-1; yybegin(EOL_COMMENT); }

	/* Separators. */
	{Separator}					{ addToken(Token.SEPARATOR); }
	{Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Operators. */
	"!" |
"!=" |
"%%" |
"%*%" |
"%/%" |
"%in%" |
"%o%" |
"%x%" |
"&" |
"&&" |
"*" |
"**" |
"+" |
"-" |
"->" |
"->>" |
"/" |
":" |
"::" |
":::" |
"<" |
"<-" |
"<<-" |
"<=" |
"=" |
"==" |
">" |
">=" |
"^" |
"|" |
"||" |
"~"		{ addToken(Token.OPERATOR); }

	/* Numbers */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexLiteral}					{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{FloatLiteral}					{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}			{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(Token.IDENTIFIER); }

}


/* No char state */

<STRING> {
	[^\"]*						{}
	[\"]						{ yybegin(YYINITIAL); addToken(start,zzStartRead, Token.LITERAL_STRING_DOUBLE_QUOTE); }
	<<EOF>>						{ addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); return firstToken; }
}


/* No multi-line comment state */

/* No documentation comment state */

<EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}

