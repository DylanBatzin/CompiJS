// ──────────────────────────────
//  Sección 1: Encabezado Java
// ──────────────────────────────
package com.example.jscompiler;

import java_cup.runtime.Symbol;

%%
// ──────────────────────────────
//  Opciones de JFlex
// ──────────────────────────────
%class JavaScriptLexer
%unicode
%cup
%public
%line
%column


// ──────────────────────────────
//  Macros
// ──────────────────────────────
LineTerminator   = \r|\n|\r\n
WhiteSpace       = {LineTerminator} | [ \t\f ]
IdentifierStart  = [a-zA-Z$_]
IdentifierPart   = [a-zA-Z0-9$_]
Identifier       = {IdentifierStart}{IdentifierPart}*

Digits           = [0-9]+
IntegerLiteral   = 0 | [1-9]{Digits}?
FloatLiteral     = {Digits}\.{Digits}? ([eE][+\-]?{Digits})?
                 | \.{Digits} ([eE][+\-]?{Digits})?
                 | {Digits}([eE][+\-]?{Digits})

StringLiteral    = \"([^\"\\]|\\.)*\" | \'([^\'\\]|\\.)*\'

%%

// ──────────────────────────────
//  Sección 2: Reglas Léxicas
// ──────────────────────────────
<YYINITIAL>{

  /* ——— Palabras clave JavaScript ——— */
  "var"      { return new Symbol(sym.VAR,      yyline, yycolumn); }
  "let"      { return new Symbol(sym.LET,      yyline, yycolumn); }
  "const"    { return new Symbol(sym.CONST,    yyline, yycolumn); }
  "let"      { return symbol(sym.LET,          yyline, yycolumn); }
  "if"       { return new Symbol(sym.IF,       yyline, yycolumn); }
  "else"     { return new Symbol(sym.ELSE,     yyline, yycolumn); }
  "while"    { return new Symbol(sym.WHILE,    yyline, yycolumn); }
  "for"      { return new Symbol(sym.FOR,      yyline, yycolumn); }
  "function" { return new Symbol(sym.FUNCTION, yyline, yycolumn); }
  "return"   { return new Symbol(sym.RETURN,   yyline, yycolumn); }

  "true"     { return new Symbol(sym.TRUE,     yyline, yycolumn, Boolean.TRUE);  }
  "false"    { return new Symbol(sym.FALSE,    yyline, yycolumn, Boolean.FALSE); }
  "null"     { return new Symbol(sym.NULL,     yyline, yycolumn, null);          }

  /* ——— Unidades de medida (para compatibilidad con el parser CUP) ——— */
  "Km"       { return new Symbol(sym.KM,       yyline, yycolumn); }
  "Hm"       { return new Symbol(sym.HM,       yyline, yycolumn); }
  "Dm"       { return new Symbol(sym.DCAM,     yyline, yycolumn); }
  "m"        { return new Symbol(sym.M,        yyline, yycolumn); }
  "dm"       { return new Symbol(sym.DCM,      yyline, yycolumn); }
  "cm"       { return new Symbol(sym.CM,       yyline, yycolumn); }
  "mm"       { return new Symbol(sym.MM,       yyline, yycolumn); }

  /* ——— Literales numéricos y de cadena ——— */
  {IntegerLiteral} { return new Symbol(sym.NUMBER, yyline, yycolumn,
                                       Integer.valueOf(yytext())); }
  {FloatLiteral}   { return new Symbol(sym.FLOAT_LITERAL,   yyline, yycolumn,
                                       Double.valueOf(yytext())); }
  {StringLiteral}  { return new Symbol(sym.STRING_LITERAL,  yyline, yycolumn,
                                       yytext().substring(1, yytext().length()-1)); }

  /* ——— Identificadores ——— */
  {Identifier}     { return new Symbol(sym.IDENTIFIER, yyline, yycolumn, yytext()); }

  /* ——— Operadores y puntuación ——— */
  "+"   { return new Symbol(sym.PLUS,          yyline, yycolumn); }
  "-"   { return new Symbol(sym.MINUS,         yyline, yycolumn); }
  "*"   { return new Symbol(sym.MULTIPLY,      yyline, yycolumn); }
  "/"   { return new Symbol(sym.DIVIDE,        yyline, yycolumn); }
  "="   { return new Symbol(sym.ASSIGN,        yyline, yycolumn); }
  "=="  { return new Symbol(sym.EQUAL,         yyline, yycolumn); }
  "===" { return new Symbol(sym.STRICT_EQUAL,  yyline, yycolumn); }
  "!="   { return new Symbol(sym.NOT_EQUAL,       yyline, yycolumn); }
  "!=="  { return new Symbol(sym.STRICT_NOT_EQUAL,yyline, yycolumn); }
  ">"    { return new Symbol(sym.GT,              yyline, yycolumn); }
  "<"    { return new Symbol(sym.LT,              yyline, yycolumn); }
  ">="   { return new Symbol(sym.GTE,             yyline, yycolumn); }
  "<="   { return new Symbol(sym.LTE,             yyline, yycolumn); }
  "&&"   { return new Symbol(sym.AND,             yyline, yycolumn); }
  "||"   { return new Symbol(sym.OR,              yyline, yycolumn); }
  "!"    { return new Symbol(sym.NOT,             yyline, yycolumn); }

  "."   { return new Symbol(sym.DOT, yyline, yycolumn); }
  ";"   { return new Symbol(sym.PUNTOCOMA,     yyline, yycolumn); }
  ","   { return new Symbol(sym.COMA,          yyline, yycolumn); }
  "("   { return new Symbol(sym.LPAREN,        yyline, yycolumn); }
  ")"   { return new Symbol(sym.RPAREN,        yyline, yycolumn); }
  "{"   { return new Symbol(sym.LBRACE,        yyline, yycolumn); }
  "}"   { return new Symbol(sym.RBRACE,        yyline, yycolumn); }

  /* ——— Espacios en blanco y comentarios: ignorar ——— */
  {WhiteSpace} { /* skip */ }

  "/*"([^*]|\*+[^*/])*\*+ "/"    { /* comentario multilinea - ignorar */ }
  "//".* { /* comentario de línea   - ignorar */ }
}

// REEMPLAZA AMBAS reglas [^] con esto:
[^] {
    String errorMsg = "Error Léxico en linea " + (yyline + 1) +
                      ", columna " + (yycolumn + 1) +
                      ": Carácter inesperado/ilegal '" + yytext() + "'";
    System.err.println(errorMsg);
    // Para detener el análisis de forma más controlada con CUP:
    return new java_cup.runtime.Symbol(sym.error, yyline, yycolumn, yytext());
    // O lanza una excepción más específica si la manejas en tu código Java
    // throw new RuntimeException(errorMsg);
}