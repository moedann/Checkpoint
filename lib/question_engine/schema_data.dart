// Generated from formats/checkpoint-question-pack.schema.json.
part of 'question_engine.dart';

final Map<String, dynamic> _packSchema =
    jsonDecode(r'''{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://checkpoint.local/schemas/question-pack-v1.json",
  "title": "Checkpoint Question Pack Draft",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "formatVersion",
    "packId",
    "title",
    "authorName",
    "locale",
    "questions"
  ],
  "properties": {
    "formatVersion": {
      "const": "1.0.0"
    },
    "packId": {
      "$ref": "#/$defs/id"
    },
    "title": {
      "$ref": "#/$defs/nonempty"
    },
    "authorName": {
      "$ref": "#/$defs/nonempty"
    },
    "locale": {
      "type": "string",
      "minLength": 2
    },
    "description": {
      "type": "string"
    },
    "source": {
      "type": "string"
    },
    "license": {
      "type": "string"
    },
    "revision": {
      "type": "integer",
      "minimum": 1
    },
    "exam": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "name": {
          "type": "string"
        },
        "level": {
          "type": "string"
        },
        "subject": {
          "type": "string"
        }
      }
    },
    "questions": {
      "type": "array",
      "minItems": 1,
      "items": {
        "oneOf": [
          {
            "$ref": "#/$defs/single"
          },
          {
            "$ref": "#/$defs/multiple"
          },
          {
            "$ref": "#/$defs/entry"
          },
          {
            "$ref": "#/$defs/grid"
          },
          {
            "$ref": "#/$defs/interaction"
          },
          {
            "$ref": "#/$defs/wordBank"
          },
          {
            "$ref": "#/$defs/selfReview"
          }
        ]
      }
    }
  },
  "$defs": {
    "id": {
      "type": "string",
      "pattern": "^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$"
    },
    "nonempty": {
      "type": "string",
      "minLength": 1
    },
    "option": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "label"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "label": {
          "$ref": "#/$defs/nonempty"
        }
      }
    },
    "common": {
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "type": "string"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        }
      }
    },
    "textField": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "mode",
        "acceptedAnswers"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "label": {
          "type": "string"
        },
        "mode": {
          "const": "text"
        },
        "acceptedAnswers": {
          "type": "array",
          "minItems": 1,
          "items": {
            "$ref": "#/$defs/nonempty"
          }
        },
        "caseSensitive": {
          "type": "boolean"
        },
        "normalizeNFKC": {
          "type": "boolean"
        },
        "trimWhitespace": {
          "type": "boolean"
        }
      }
    },
    "numberField": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "mode",
        "expectedValue"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "label": {
          "type": "string"
        },
        "mode": {
          "const": "number"
        },
        "expectedValue": {
          "type": "number"
        },
        "absoluteTolerance": {
          "type": "number",
          "minimum": 0
        },
        "relativeTolerance": {
          "type": "number",
          "minimum": 0
        },
        "unit": {
          "type": "string"
        }
      }
    },
    "single": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "options",
        "answerKey"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "single_choice"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "options": {
          "type": "array",
          "minItems": 2,
          "items": {
            "$ref": "#/$defs/option"
          }
        },
        "answerKey": {
          "type": "object",
          "additionalProperties": false,
          "required": [
            "optionId"
          ],
          "properties": {
            "optionId": {
              "$ref": "#/$defs/id"
            }
          }
        },
        "shuffleOptions": {
          "type": "boolean"
        }
      }
    },
    "multiple": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "options",
        "answerKey"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "multiple_choice"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "options": {
          "type": "array",
          "minItems": 2,
          "items": {
            "$ref": "#/$defs/option"
          }
        },
        "minSelections": {
          "type": "integer",
          "minimum": 0
        },
        "maxSelections": {
          "type": "integer",
          "minimum": 1
        },
        "answerKey": {
          "type": "object",
          "additionalProperties": false,
          "required": [
            "optionIds"
          ],
          "properties": {
            "optionIds": {
              "type": "array",
              "minItems": 1,
              "uniqueItems": true,
              "items": {
                "$ref": "#/$defs/id"
              }
            }
          }
        }
      }
    },
    "entry": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "fields"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "entry"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "fields": {
          "type": "array",
          "minItems": 1,
          "items": {
            "oneOf": [
              {
                "$ref": "#/$defs/textField"
              },
              {
                "$ref": "#/$defs/numberField"
              }
            ]
          }
        }
      }
    },
    "grid": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "columns",
        "rows"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "grid"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "columns": {
          "type": "array",
          "minItems": 1,
          "items": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "id",
              "title"
            ],
            "properties": {
              "id": {
                "$ref": "#/$defs/id"
              },
              "title": {
                "type": "string"
              }
            }
          }
        },
        "rows": {
          "type": "array",
          "minItems": 1,
          "items": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "id",
              "title",
              "cells"
            ],
            "properties": {
              "id": {
                "$ref": "#/$defs/id"
              },
              "title": {
                "type": "string"
              },
              "cells": {
                "type": "array",
                "minItems": 1,
                "items": {
                  "oneOf": [
                    {
                      "type": "object",
                      "additionalProperties": false,
                      "required": [
                        "columnId",
                        "mode",
                        "value"
                      ],
                      "properties": {
                        "columnId": {
                          "$ref": "#/$defs/id"
                        },
                        "mode": {
                          "const": "static"
                        },
                        "value": {
                          "type": "string"
                        }
                      }
                    },
                    {
                      "type": "object",
                      "additionalProperties": false,
                      "required": [
                        "columnId",
                        "mode",
                        "acceptedAnswers"
                      ],
                      "properties": {
                        "columnId": {
                          "$ref": "#/$defs/id"
                        },
                        "mode": {
                          "const": "text"
                        },
                        "acceptedAnswers": {
                          "type": "array",
                          "minItems": 1,
                          "items": {
                            "$ref": "#/$defs/nonempty"
                          }
                        }
                      }
                    },
                    {
                      "type": "object",
                      "additionalProperties": false,
                      "required": [
                        "columnId",
                        "mode",
                        "expectedValue"
                      ],
                      "properties": {
                        "columnId": {
                          "$ref": "#/$defs/id"
                        },
                        "mode": {
                          "const": "number"
                        },
                        "expectedValue": {
                          "type": "number"
                        },
                        "absoluteTolerance": {
                          "type": "number",
                          "minimum": 0
                        },
                        "relativeTolerance": {
                          "type": "number",
                          "minimum": 0
                        }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
      }
    },
    "interaction": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "mode",
        "items",
        "answerKey"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "interaction"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "mode": {
          "enum": [
            "ordering",
            "grouping"
          ]
        },
        "items": {
          "type": "array",
          "minItems": 2,
          "items": {
            "$ref": "#/$defs/option"
          }
        },
        "groups": {
          "type": "array",
          "minItems": 2,
          "items": {
            "$ref": "#/$defs/option"
          }
        },
        "answerKey": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "orderedIds": {
              "type": "array",
              "minItems": 2,
              "uniqueItems": true,
              "items": {
                "$ref": "#/$defs/id"
              }
            },
            "assignments": {
              "type": "array",
              "minItems": 2,
              "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "itemId",
                  "groupId"
                ],
                "properties": {
                  "itemId": {
                    "$ref": "#/$defs/id"
                  },
                  "groupId": {
                    "$ref": "#/$defs/id"
                  }
                }
              }
            }
          }
        }
      }
    },
    "wordBank": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "segments",
        "tokens",
        "answerKey"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "word_bank"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "segments": {
          "type": "array",
          "minItems": 1,
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "value"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "value": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "id"
                ],
                "properties": {
                  "kind": {
                    "const": "blank"
                  },
                  "id": {
                    "$ref": "#/$defs/id"
                  }
                }
              }
            ]
          }
        },
        "tokens": {
          "type": "array",
          "minItems": 1,
          "items": {
            "$ref": "#/$defs/option"
          }
        },
        "reuseTokens": {
          "type": "boolean"
        },
        "answerKey": {
          "type": "object",
          "additionalProperties": false,
          "required": [
            "matches"
          ],
          "properties": {
            "matches": {
              "type": "array",
              "minItems": 1,
              "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "blankId",
                  "tokenId"
                ],
                "properties": {
                  "blankId": {
                    "$ref": "#/$defs/id"
                  },
                  "tokenId": {
                    "$ref": "#/$defs/id"
                  }
                }
              }
            }
          }
        }
      }
    },
    "selfReview": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "id",
        "type",
        "prompt",
        "explanation",
        "modelAnswer"
      ],
      "properties": {
        "id": {
          "$ref": "#/$defs/id"
        },
        "type": {
          "const": "self_review"
        },
        "prompt": {
          "$ref": "#/$defs/nonempty"
        },
        "explanation": {
          "$ref": "#/$defs/nonempty"
        },
        "tags": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string"
          }
        },
        "source": {
          "type": "string"
        },
        "groupId": {
          "type": "string"
        },
        "stimulus": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "text"
                ],
                "properties": {
                  "kind": {
                    "const": "text"
                  },
                  "text": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "latex"
                ],
                "properties": {
                  "kind": {
                    "const": "math"
                  },
                  "latex": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId",
                  "alt"
                ],
                "properties": {
                  "kind": {
                    "const": "image"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "alt": {
                    "type": "string"
                  }
                }
              },
              {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "kind",
                  "assetId"
                ],
                "properties": {
                  "kind": {
                    "const": "audio"
                  },
                  "assetId": {
                    "$ref": "#/$defs/id"
                  },
                  "transcript": {
                    "type": "string"
                  }
                }
              }
            ]
          }
        },
        "modelAnswer": {
          "$ref": "#/$defs/nonempty"
        },
        "reviewRubric": {
          "type": "string"
        },
        "activityMode": {
          "enum": [
            "paper",
            "pc",
            "oral",
            "practical",
            "essay",
            "other"
          ]
        }
      }
    }
  }
}
''')
        as Map<String, dynamic>;
